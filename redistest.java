@RestController
@RequestMapping("/redis")
@Slf4j
public class RedisConnectivityController {

    @GetMapping("/ping")
    public ResponseEntity<String> pingRedis() {

        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(
                            "snd-ticketing-valkey-x4ubek.serverless.use1.cache.amazonaws.com",
                            6379),
                    5000);

            return ResponseEntity.ok(
                    "TCP Connection Successful");

        } catch (Exception ex) {

            return ResponseEntity.internalServerError()
                    .body("Connection Failed : "
                            + ex.getMessage());
        }
    }
}
