aws ssm start-session \
    --target i-YOUR_EC2_INSTANCE_ID \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"portNumber\":[\"6379\"],\"localPortNumber\":[\"6379\"],\"host\":[\"snd-ticketing-valkey-x4ubek.serverless.use1.cache.amazonaws.com\"]}"

aws ec2 describe-instances --filters "Name=instance-state-name,Values=running" --query "Reservations[*].Instances[*].[InstanceId,Tags[?Key=='Name'].Value|[0]]" --output table

