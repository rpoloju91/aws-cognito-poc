aws ssm start-session \
    --target i-YOUR_EC2_INSTANCE_ID \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"portNumber\":[\"6379\"],\"localPortNumber\":[\"6379\"],\"host\":[\""]}"

aws ec2 describe-instances --filters "Name=instance-state-name,Values=running" --query "Reservations[*].Instances[*].[InstanceId,Tags[?Key=='Name'].Value|[0]]" --output table

redis:
  host: 
  port: 6379
  user-id: 
  serverless-cache-name: 
  region: us-east-1
