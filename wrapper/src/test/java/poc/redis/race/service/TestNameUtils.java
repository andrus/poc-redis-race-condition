package poc.redis.race.service;


import org.junit.rules.TestName;

public class TestNameUtils {

	public static String testName(TestName name) {
		String methodName = name.getClass().getName() + " > " + name.getMethodName();
		return paddedString(methodName, 80, " === [ ", " ] ===", '=', " ");
	}

	public static String paddedString(String name, int length, String prefix, String postfix, Character padder, String terminator) {
		if (prefix == null) prefix = "";
		if (postfix == null) postfix = "";
		if (terminator == null) terminator = "";
		final int targetLength = length - terminator.length();
		StringBuilder sb = new StringBuilder(prefix);
		sb.append(name);
		sb.append(postfix);
		
		if (sb.length() >= targetLength) {
			return sb.append(terminator).toString();
		}
		
		while (sb.length() < targetLength) {
			sb.append(padder);
		}

		return sb.append(terminator).toString();
	}
}
