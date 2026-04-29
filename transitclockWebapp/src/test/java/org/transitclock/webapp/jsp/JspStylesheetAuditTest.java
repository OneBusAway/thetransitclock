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
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * Pins the Tailwind delivery path so a future edit can't silently regress to
 * the in-browser Play CDN. The Vite build emits {@code dist/tailwind.css}
 * and includes.jsp links it; if either stops being true, every page renders
 * unstyled (the page returns 200 — Tailwind classes simply don't apply).
 */
public class JspStylesheetAuditTest {

	private static final Path WEBAPP_ROOT = Paths.get("src/main/webapp");

	@Test
	public void noJspReferencesTailwindPlayCdn() throws IOException {
		List<Path> offenders = new ArrayList<>();

		try (Stream<Path> stream = Files.walk(WEBAPP_ROOT)) {
			stream.filter(p -> {
						String n = p.toString();
						return n.endsWith(".jsp") || n.endsWith(".tag");
					})
					.forEach(jsp -> {
						String content = readSilently(jsp);
						if (content.contains("@tailwindcss/browser")
								|| content.contains("cdn.tailwindcss.com")) {
							offenders.add(jsp);
						}
					});
		}

		assertThat(offenders)
				.as("JSPs/tags loading Tailwind from the Play CDN — Tailwind "
						+ "now ships from the Vite build at /dist/tailwind.css")
				.isEmpty();
	}

	@Test
	public void includesJspLinksBundledTailwind() throws IOException {
		Path includes = WEBAPP_ROOT.resolve("template/includes.jsp");
		assertThat(includes).as("template/includes.jsp must exist").exists();
		String content = readSilently(includes);
		assertThat(content)
				.as("includes.jsp must link the Vite-built /dist/tailwind.css")
				.contains("/dist/tailwind.css");
	}

	private static String readSilently(Path p) {
		try {
			return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Could not read " + p, e);
		}
	}
}
