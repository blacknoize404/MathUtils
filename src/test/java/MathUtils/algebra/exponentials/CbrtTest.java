package MathUtils.algebra.exponentials;

import MathUtils.algebra.Var;
import MathUtils.algebra.primitives.MathElement;
import org.junit.jupiter.api.Test;

class CbrtTest {

    @Test
    void equalsBetweenSqrtAndRootWithArgument3() {

        MathElement variable = new Var("x");
        MathElement root = new Cbrt(variable);

        // Cuando es igual a sí mismo
        assert root.equalsTo(root);

        // Cuando son iguales
        MathElement root2 = new Root(variable, 3);
        assert root.equalsTo(root2);

        // Cuando se compara en ambos sentidos
        assert root.equalsTo(root2) && root2.equalsTo(root);

        // Cuando no lo son
        MathElement root3 = new Root(variable, 2);
        assert !root.equalsTo(root3);

        // Cuando se compara en ambos sentidos valores que no son iguales
        assert !root.equalsTo(root3) && !root3.equalsTo(root);


    }

}