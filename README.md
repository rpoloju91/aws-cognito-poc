aws ssm start-session \
    --target i-YOUR_EC2_INSTANCE_ID \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"portNumber\":[\"6379\"],\"localPortNumber\":[\"6379\"],\"host\":[\"snd-ticketing-valkey-x4ubek.serverless.use1.cache.amazonaws.com\"]}"
