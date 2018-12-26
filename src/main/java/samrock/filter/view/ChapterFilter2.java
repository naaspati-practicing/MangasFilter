package samrock.filter.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sam.manga.samrock.chapters.ChapterFilter;

public class ChapterFilter2 extends ChapterFilter {
	private List<Double> numbers = new ArrayList<>();

	public ChapterFilter2(int manga_id, String title) {
		super(manga_id, title);
	}

	@Override
	public void add(double value) {
		super.add(value);
		numbers.add(value);
	}
	public List<Double> all() {
		return Collections.unmodifiableList(numbers);
	}
}
