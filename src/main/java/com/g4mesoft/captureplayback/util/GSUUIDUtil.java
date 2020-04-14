package com.g4mesoft.captureplayback.util;

import java.util.UUID;
import java.util.function.Predicate;

public class GSUUIDUtil {

	public static UUID randomUnique(Predicate<UUID> existsPredicate) {
		UUID result;
		do {
			result = UUID.randomUUID();
		} while (existsPredicate.test(result));
		
		return result;
	}
}
