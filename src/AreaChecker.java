import java.math.BigDecimal;

public class AreaChecker {
    public static boolean isHit(BigDecimal x, BigDecimal y, BigDecimal r) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal halfR = r.divide(BigDecimal.valueOf(2));
        boolean inRectangle = x.compareTo(r.negate()) >= 0 && x.compareTo(zero) <= 0 &&
                y.compareTo(halfR.negate()) >= 0 && y.compareTo(zero) <= 0;
        boolean inTriangle = x.compareTo(r.negate()) >= 0 && x.compareTo(zero) <= 0 &&
                y.compareTo(zero) >= 0 && y.compareTo(x.add(r).divide(BigDecimal.valueOf(2))) <= 0;
        boolean inQuarterCircle = x.compareTo(zero) >= 0 && y.compareTo(zero) >= 0 &&
                x.multiply(x).add(y.multiply(y)).compareTo(halfR.multiply(halfR)) <= 0;
        return inRectangle || inTriangle || inQuarterCircle;
    }
}
