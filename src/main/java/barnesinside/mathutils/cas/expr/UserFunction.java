package barnesinside.mathutils.cas.expr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Función definida por el usuario, por ejemplo {@code f(x, y) = x^2 + y}.
 *
 * @param parameters nombres de los parámetros, en orden
 * @param body       cuerpo de la función en función de los parámetros
 */
public record UserFunction(List<String> parameters, Expr body) {

    public UserFunction {
        parameters = List.copyOf(parameters);
    }

    /**
     * Aplica la función a unos argumentos sustituyendo los parámetros en el cuerpo.
     * @throws IllegalArgumentException si la cantidad de argumentos no coincide
     */
    public Expr apply(List<Expr> arguments) {
        if (arguments.size() != parameters.size()) {
            throw new IllegalArgumentException(
                    "La función espera " + parameters.size() + " argumento(s) y recibió " + arguments.size());
        }
        Map<String, Expr> bindings = new HashMap<>();
        for (int i = 0; i < parameters.size(); i++) bindings.put(parameters.get(i), arguments.get(i));
        return body.substitute(bindings);
    }
}
