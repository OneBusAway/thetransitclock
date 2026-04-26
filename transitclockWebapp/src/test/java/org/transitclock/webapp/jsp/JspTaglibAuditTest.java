/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.webapp.jsp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * Audits every JSP in {@code src/main/webapp} for {@code <%@ taglib %>} URIs
 * and asserts each one is in the project's expected set. Tomcat 11 ships
 * Jakarta Tags 3.0 which only resolves {@code jakarta.tags.core} and
 * {@code jakarta.tags.fmt}. The legacy {@code http://java.sun.com/jsp/jstl/*}
 * URIs silently fail to resolve — pages render blank without an exception —
 * so a CI-time text audit is the only practical defense if a JSP regresses
 * back to the old namespace.
 */
public class JspTaglibAuditTest {

	/** URIs the project currently uses (Phase B: Jakarta Tags 3.0). */
	private static final Set<String> EXPECTED_URIS = new HashSet<>();
	static {
		EXPECTED_URIS.add("jakarta.tags.core");
		EXPECTED_URIS.add("jakarta.tags.fmt");
	}

	/** Pattern matching {@code <%@ taglib prefix="..." uri="..." %>}. */
	private static final Pattern TAGLIB_DIRECTIVE = Pattern.compile(
			"<%@\\s*taglib\\b[^%]*?\\buri\\s*=\\s*\"([^\"]+)\"",
			Pattern.MULTILINE);

	private static final Path WEBAPP_ROOT = Paths.get("src/main/webapp");

	@Test
	public void everyJspTaglibUriIsExpected() throws IOException {
		List<Violation> violations = new ArrayList<>();

		try (Stream<Path> stream = Files.walk(WEBAPP_ROOT)) {
			stream.filter(p -> p.toString().endsWith(".jsp"))
					.forEach(jsp -> {
						String content = readSilently(jsp);
						Matcher m = TAGLIB_DIRECTIVE.matcher(content);
						while (m.find()) {
							String uri = m.group(1);
							if (!EXPECTED_URIS.contains(uri)) {
								violations.add(new Violation(jsp, uri));
							}
						}
					});
		}

		assertThat(violations)
				.as("JSPs declaring taglib URIs not in EXPECTED_URIS — Tomcat 11 "
						+ "Jakarta Tags 3.0 only resolves jakarta.tags.core / "
						+ "jakarta.tags.fmt; legacy http://java.sun.com/jsp/jstl/* "
						+ "would render blank pages")
				.isEmpty();
	}

	@Test
	public void jspsWithTaglibsExist() throws IOException {
		// Sanity check: if no JSPs declare taglibs, the audit above is
		// vacuously green and would silently miss a wholesale taglib removal.
		long jspsWithTaglibs;
		try (Stream<Path> stream = Files.walk(WEBAPP_ROOT)) {
			jspsWithTaglibs = stream
					.filter(p -> p.toString().endsWith(".jsp"))
					.filter(p -> TAGLIB_DIRECTIVE.matcher(readSilently(p)).find())
					.count();
		}
		assertThat(jspsWithTaglibs).isGreaterThanOrEqualTo(10);
	}

	private static String readSilently(Path p) {
		try {
			return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Could not read " + p, e);
		}
	}

	private static final class Violation {
		final Path file;
		final String uri;

		Violation(Path file, String uri) {
			this.file = file;
			this.uri = uri;
		}

		@Override
		public String toString() {
			return file + " uses unexpected taglib uri \"" + uri + "\"";
		}
	}
}
