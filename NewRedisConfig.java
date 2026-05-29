@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${redis.user-id}")
    private String userId;

    @Value("${redis.replication-group-id}")
    private String replicationGroupId;

    @Value("${redis.region}")
    private String region;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        AwsCredentialsProvider awsCredentialsProvider =
                DefaultCredentialsProvider.create();

        IAMAuthTokenRequest tokenRequest =
                new IAMAuthTokenRequest(
                        userId,
                        replicationGroupId,
                        region);

        RedisCredentialsProvider credentialsProvider =
                new RedisIAMAuthCredentialsProvider(
                        userId,
                        tokenRequest,
                        awsCredentialsProvider);

        RedisURI redisURI = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withSsl(true)
                .withAuthentication(credentialsProvider)
                .build();

        RedisClient redisClient = RedisClient.create(redisURI);

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(
                        new RedisStandaloneConfiguration(
                                redisHost,
                                redisPort));

        factory.afterPropertiesSet();

        return factory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(
            RedisConnectionFactory factory) {

        RedisTemplate<String, String> template =
                new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(
                new StringRedisSerializer());

        template.setValueSerializer(
                new StringRedisSerializer());

        return template;
    }
}
