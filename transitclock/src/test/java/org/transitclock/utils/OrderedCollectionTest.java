package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * OrderedCollection merges several ordered stop lists into a single stop list
 * while preserving order. It's used to build a direction's stop list across
 * multiple trip patterns, so insertion placement is the behavior that matters.
 * The public API only exposes addOriginal() + add(List&lt;List&gt;) + get(), so
 * these tests drive the full merge through add(List&lt;List&gt;).
 */
public class OrderedCollectionTest {

	/**
	 * Builds a mutable list-of-lists so add() can sort it in place.
	 */
	@SafeVarargs
	private static List<List<String>> lists(List<String>... arrays) {
		List<List<String>> out = new ArrayList<>();
		for (List<String> l : arrays) {
			out.add(l);
		}
		return out;
	}

	@Test
	public void emptyCollectionReturnsEmptyList() {
		OrderedCollection oc = new OrderedCollection();
		assertTrue(oc.get().isEmpty());
	}

	@Test
	public void addOriginalSeedsListInOrder() {
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("a", "b", "c", "d"));
		assertEquals(Arrays.asList("a", "b", "c", "d"), oc.get());
	}

	@Test
	public void identicalListDoesNotDuplicate() {
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("a", "b", "c"));
		oc.add(lists(Arrays.asList("a", "b", "c")));
		assertEquals(Arrays.asList("a", "b", "c"), oc.get());
	}

	@Test
	public void newItemsInsertedBetweenAnchors() {
		// New list inserts "b.1", "b.2" between existing "b" and "c".
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("a", "b", "c", "d"));
		oc.add(lists(Arrays.asList("a", "b", "b.1", "b.2", "c", "d")));
		assertEquals(
				Arrays.asList("a", "b", "b.1", "b.2", "c", "d"),
				oc.get());
	}

	@Test
	public void newItemsAppendedWhenNoAnchorFollows() {
		// New list adds tail items after a known anchor.
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("a", "b", "c"));
		oc.add(lists(Arrays.asList("a", "b", "c", "d", "e")));
		assertEquals(Arrays.asList("a", "b", "c", "d", "e"), oc.get());
	}

	@Test
	public void newItemsPrependedWhenNoAnchorPrecedes() {
		// New list prepends items before an existing anchor.
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("c", "d"));
		oc.add(lists(Arrays.asList("a", "b", "c", "d")));
		assertEquals(Arrays.asList("a", "b", "c", "d"), oc.get());
	}

	@Test
	public void listsAreProcessedInSizeDescendingOrder() {
		// A shorter list processed first would miss anchors from the longer
		// list. add(listOfLists) sorts by size descending so the longest list
		// acts as the scaffold. We verify by including a mutable list whose
		// order would produce different results if processed first-to-last.
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("1", "2", "3", "4"));
		// Smaller list ("1", "x", "4") inserts "x" between 1 and 4. The larger
		// list ("1", "2", "y", "3", "4") inserts "y" between 2 and 3. If the
		// smaller ran first, "x" would land in a position relative to 1/4
		// only; the larger still anchors correctly.
		oc.add(lists(
				Arrays.asList("1", "x", "4"),
				Arrays.asList("1", "2", "y", "3", "4")));
		// Size-desc sort means the 5-element list runs first, then the 3-elem.
		// After first: [1, 2, y, 3, 4]. After second: "x" must go between 1
		// and 4 — it lands right after the first anchor "1".
		assertEquals(
				Arrays.asList("1", "x", "2", "y", "3", "4"),
				oc.get());
	}

	@Test
	public void loopingRouteHandlesRepeatedItem() {
		// Routes that loop back to a starting stop cause the same item to
		// appear multiple times in a trip's stop list. indexWhereItemExists()
		// starts searching from the current insertion point so it anchors to
		// the next occurrence rather than rewinding.
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("1", "2", "3", "4"));
		oc.add(lists(Arrays.asList("3", "4", "1", "2", "3")));
		// Walking the list: "3" anchors at index 2. "4" anchors at index 3.
		// "1" is not found after index 4, so it's queued. "2" queued. "3"
		// queued. End of input — queued items append to the end.
		assertEquals(
				Arrays.asList("1", "2", "3", "4", "1", "2", "3"),
				oc.get());
	}

	@Test
	public void multipleListsMergeIntoSingleOrdering() {
		// Reproduces the spirit of the main() demo: a scaffold plus two
		// branches that insert distinct stops at different points.
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("1", "2", "3", "4"));
		oc.add(lists(
				Arrays.asList("a21", "a22", "1", "2", "3"),
				Arrays.asList("1", "2", "a31", "a32", "3", "4", "a35", "a36")));
		List<String> result = oc.get();
		// Every original + branch item is present.
		assertTrue(result.containsAll(Arrays.asList(
				"1", "2", "3", "4",
				"a21", "a22",
				"a31", "a32", "a35", "a36")));
		// Relative ordering: anchors stay in their original order, branches
		// attach around them.
		assertTrue(result.indexOf("a21") < result.indexOf("1"));
		assertTrue(result.indexOf("a22") < result.indexOf("1"));
		assertTrue(result.indexOf("2") < result.indexOf("a31"));
		assertTrue(result.indexOf("a32") < result.indexOf("3"));
		assertTrue(result.indexOf("4") < result.indexOf("a35"));
		assertTrue(result.indexOf("a35") < result.indexOf("a36"));
	}

	@Test
	public void addEmptyListOfListsIsNoop() {
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("a", "b"));
		oc.add(Collections.<List<String>>emptyList());
		assertEquals(Arrays.asList("a", "b"), oc.get());
	}

	@Test
	public void toStringIncludesListContents() {
		OrderedCollection oc = new OrderedCollection();
		oc.addOriginal(Arrays.asList("x", "y"));
		String s = oc.toString();
		assertTrue(s.contains("x"));
		assertTrue(s.contains("y"));
	}
}
