package io.github.ozkanpakdil.grepwise.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the API version for a controller or a specific endpoint.
 * This annotation can be applied to classes or methods.
 * 
 * Example usage:
 * 
 * <pre>
 * {@code
 * @RestController
 * @RequestMapping("/api/users")
 * @ApiVersion(1)
 * public class UserController {
 *     // This controller will be accessible at /api/v1/users
 * }
 * 
 * @RestController
 * @RequestMapping("/api/users")
 * public class UserController {
 *     
 *     @GetMapping
 *     @ApiVersion(1)
 *     public ResponseEntity<List<User>> getUsersV1() {
 *         // This method will be accessible at /api/v1/users
 *     }
 *     
 *     @GetMapping
 *     @ApiVersion(2)
 *     public ResponseEntity<List<UserV2>> getUsersV2() {
 *         // This method will be accessible at /api/v2/users
 *     }
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    
    /**
     * The version number for the API.
     * 
     * @return the version number
     */
    int value();
}