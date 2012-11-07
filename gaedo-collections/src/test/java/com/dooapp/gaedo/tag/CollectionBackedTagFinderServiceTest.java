package com.dooapp.gaedo.tag;

import java.util.List;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.dooapp.gaedo.finders.FieldInformer;
import com.dooapp.gaedo.finders.FinderCrudService;
import com.dooapp.gaedo.finders.QueryBuilder;
import com.dooapp.gaedo.finders.QueryExpression;
import com.dooapp.gaedo.finders.expressions.Expressions;
import com.dooapp.gaedo.finders.informers.StringFieldInformer;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.test.beans.Tag;
import com.dooapp.gaedo.test.beans.TagInformer;

public class CollectionBackedTagFinderServiceTest {
	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private FinderCrudService<Tag, TagInformer> tagService;

	@Before
	public void loadService() {
		ServiceRepository repository = TestEnvironmentProvider.create();
		tagService = repository.get(Tag.class);
	}

	@Test
	public void testCreate() {
		Tag input = new Tag(A);
		Tag returned = tagService.create(input);
		Assert.assertEquals(returned.getText(), input.getText());
	}

	@Test
	public void testFindByName() {
		Tag a = new Tag(A);
		tagService.create(a);
		Tag b = tagService.create(new Tag(B));
		Tag c = tagService.create(new Tag(C));
		Iterable<Tag> values = tagService.find().matching(
				new QueryBuilder<TagInformer>() {

					public QueryExpression createMatchingExpression(
							TagInformer object) {
						return object.get("text").equalsTo(A);
					}
				}).getAll();
		Assert.assertThat(values, IsInstanceOf.instanceOf(List.class));
		List<Tag> valuesAscollection = (List<Tag>) values;
		Assert.assertThat(valuesAscollection.size(), Is.is(1));
		Assert.assertThat(valuesAscollection.get(0), Is.is(a));
	}

	@Test
	public void testFindUsingEqualsTo() {
		final Tag a = new Tag(A);
		tagService.create(a);
		Tag b = tagService.create(new Tag(B));
		Tag c = tagService.create(new Tag(C));
		Iterable<Tag> values = tagService.find().matching(
				new QueryBuilder<TagInformer>() {

					public QueryExpression createMatchingExpression(
							TagInformer object) {
						return object.equalsTo(a);
					}
				}).getAll();
		Assert.assertThat(values, IsInstanceOf.instanceOf(List.class));
		List<Tag> valuesAscollection = (List<Tag>) values;
		Assert.assertThat(valuesAscollection.size(), Is.is(1));
		Assert.assertThat(valuesAscollection.get(0), Is.is(a));
	}

	@Test
	public void testFindByTwoPossibleNames() {
		Tag a = new Tag(A);
		tagService.create(a);
		Tag b = tagService.create(new Tag(B));
		Tag c = tagService.create(new Tag(C));
		Iterable<Tag> values = tagService.find().matching(
				new QueryBuilder<TagInformer>() {

					public QueryExpression createMatchingExpression(
							TagInformer object) {
						FieldInformer field = object.get("text");
						return Expressions.or(field.equalsTo(A), field
								.equalsTo(B));
					}
				}).getAll();
		Assert.assertThat(values, IsInstanceOf.instanceOf(List.class));
		List<Tag> valuesAscollection = (List<Tag>) values;
		Assert.assertThat(valuesAscollection.size(), Is.is(2));
		// Here, beware to elements insertion order, due to collection class
		Assert.assertThat(valuesAscollection.get(0), Is.is(a));
		Assert.assertThat(valuesAscollection.get(1), Is.is(b));
	}

	/**
	 * This is the deadly test that check that synthetic getter calls work
	 * correctly
	 */
	@Test
	public void testFindByTwoPossibleNamesWithSyntheticAccessor() {
		Tag a = new Tag(A);
		tagService.create(a);
		Tag b = tagService.create(new Tag(B));
		Tag c = tagService.create(new Tag(C));
		Iterable<Tag> values = tagService.find().matching(
				new QueryBuilder<TagInformer>() {

					public QueryExpression createMatchingExpression(
							TagInformer object) {
						StringFieldInformer field = object.getText();
						return Expressions.or(field.equalsTo(A), field
								.equalsTo(B));
					}
				}).getAll();
		Assert.assertThat(values, IsInstanceOf.instanceOf(List.class));
		List<Tag> valuesAscollection = (List<Tag>) values;
		Assert.assertThat(valuesAscollection.size(), Is.is(2));
		// Here, beware to elements insertion order, due to collection class
		Assert.assertThat(valuesAscollection.get(0), Is.is(a));
		Assert.assertThat(valuesAscollection.get(1), Is.is(b));
	}
}
