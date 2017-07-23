package nickel.util;

import java.util.function.Predicate;

/**
 * Created by Murray on 21/07/2017
 */
public class LambdaUtil {

    public static<T> Predicate<T> not(Predicate<T> p) {
        return t -> !p.test(t);
    }
}
