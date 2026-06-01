from typing import Tuple
from urllib.parse import ParseResult, urlencode, urlunparse

import asyncio
import botocore.session

from botocore.model import ServiceId
from botocore.signers import RequestSigner
from cachetools import TTLCache, cached

from glide import (
    GlideClusterClient,
    GlideClusterClientConfiguration,
    ServerCredentials,
    NodeAddress,
)


class ElastiCacheIAMProvider:

    def __init__(self, user, cluster_name, region="us-east-1"):
        self.user = user
        self.cluster_name = cluster_name
        self.region = region

        session = botocore.session.get_session()

        self.request_signer = RequestSigner(
            ServiceId("elasticache"),
            region,
            "elasticache",
            "v4",
            session.get_credentials(),
            session.get_component("event_emitter"),
        )

    @cached(cache=TTLCache(maxsize=128, ttl=900))
    def get_credentials(self) -> Tuple[str, str]:

        query_params = {
            "Action": "connect",
            "User": self.user,
        }

        url = urlunparse(
            ParseResult(
                scheme="https",
                netloc=self.cluster_name,
                path="/",
                query=urlencode(query_params),
                params="",
                fragment="",
            )
        )

        signed_url = self.request_signer.generate_presigned_url(
            {
                "method": "GET",
                "url": url,
                "body": {},
                "headers": {},
                "context": {},
            },
            operation_name="connect",
            expires_in=900,
            region_name=self.region,
        )

        return self.user, signed_url.removeprefix("https://")


async def main():

    USERNAME = "hl-snd-ticketing-v1-AnywhereRole"
    CLUSTER_NAME = "snd-ticketing-valkey"
    HOST = "snd-ticketing-valkey-x4ubek.serverless.use1.cache.amazonaws.com"

    auth = ElastiCacheIAMProvider(
        user=USERNAME,
        cluster_name=CLUSTER_NAME,
        region="us-east-1"
    )

    username, token = auth.get_credentials()

    print("Token Generated")
    print(token[:100])

    credentials = ServerCredentials(
        username=username,
        password=token,
    )

    config = GlideClusterClientConfiguration(
        addresses=[NodeAddress(HOST, 6379)],
        use_tls=True,
        credentials=credentials,
    )

    client = await GlideClusterClient.create(config)

    result = await client.ping()

    print("PING:", result)


if __name__ == "__main__":
    asyncio.run(main())
