package com.dooapp.gaedo.blueprints;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.dooapp.gaedo.blueprints.beans.PostSubClass;
import com.dooapp.gaedo.finders.QueryBuilder;
import com.dooapp.gaedo.finders.QueryExpression;
import com.dooapp.gaedo.finders.id.IdBasedService;
import com.dooapp.gaedo.test.beans.Post;
import com.dooapp.gaedo.test.beans.PostInformer;
import com.dooapp.gaedo.test.beans.State;
import com.dooapp.gaedo.test.beans.Tag;
import com.dooapp.gaedo.test.beans.User;

import static com.dooapp.gaedo.blueprints.TestUtils.ABOUT_ID;
import static com.dooapp.gaedo.blueprints.TestUtils.ID_POST_1;
import static com.dooapp.gaedo.blueprints.TestUtils.TAG_TEXT;
import static com.dooapp.gaedo.blueprints.TestUtils.USER_LOGIN;
import static com.dooapp.gaedo.blueprints.TestUtils.USER_PASSWORD;
import static com.dooapp.gaedo.blueprints.TestUtils.simpleTest;

import static org.junit.Assert.assertThat;


@RunWith(Parameterized.class)
public class GraphPostSubClassFinderServiceTest extends AbstractGraphTest{
	private static final long SUB_POST_ID = 1000+ID_POST_1;
	private static final Logger logger = Logger.getLogger(GraphPostSubClassFinderServiceTest.class.getName());
	
	@Parameters
	public static Collection<Object[]> parameters() {
		return simpleTest();
	}

	private User author;
	private Post post1;
	private Post post2;
	private Post post3;
	private Tag tag1;
	
	public GraphPostSubClassFinderServiceTest(AbstractGraphEnvironment<?> environment) {
		super(environment);
		// TODO Auto-generated constructor stub
	}

	@Before
	public void loadService() throws Exception {
		super.loadService();

		// create some objects
		author = new User().withId(1).withLogin(USER_LOGIN).withPassword(USER_PASSWORD);
		author.about = new Post(ABOUT_ID, "a message about that user", 5, State.PUBLIC, author);
		author = getUserService().create(author);
		tag1 = getTagService().create(new Tag(1, TAG_TEXT));
	}
	
	private Map<String, String> theseMappings(String...strings) {
		Map<String, String> returned = new TreeMap<String, String>();
		for (int i = 0; i < strings.length; i++) {
			if(i+1<strings.length) {
				returned.put(strings[i++], strings[i]);
			}
		}
		return returned;
	}

	@Test 
	public void ensurePostSubClassServiceWorksWell() {
		final String METHOD_NAME = "ensurePostSubClassServiceWorksWell";
		PostSubClass newOne = new PostSubClass(SUB_POST_ID, METHOD_NAME,1.0f, State.PUBLIC, author);
		Post saved = getPostSubService().create(newOne);
		assertThat(saved, Is.is(PostSubClass.class));
		Post loaded = getPostService().find().matching(new QueryBuilder<PostInformer>() {
			
			@Override
			public QueryExpression createMatchingExpression(PostInformer informer) {
				return informer.getText().equalsTo(METHOD_NAME);
			}
		}).getFirst();
		assertThat(loaded, Is.is(PostSubClass.class));
		if(getPostService() instanceof IdBasedService) {
			Post fromId = ((IdBasedService<Post>) getPostService()).findById(loaded.id);
			assertThat(fromId, Is.is(PostSubClass.class));
		}
		// Now make sure post can be updated and obtained
		loaded.text = "updated text : "+loaded.text;
		// exposition of https://github.com/Riduidel/gaedo/issues/23 here !
		// by updating object with basic post service, I create a second vertex in graph i can then retrieve in count.
		loaded = getPostService().update(loaded);
		assertThat(getPostService().find().matching(new QueryBuilder<PostInformer>() {

			@Override
			public QueryExpression createMatchingExpression(PostInformer informer) {
				return informer.getId().equalsTo(SUB_POST_ID);
			}
		}).count(), Is.is(1));
	}

}