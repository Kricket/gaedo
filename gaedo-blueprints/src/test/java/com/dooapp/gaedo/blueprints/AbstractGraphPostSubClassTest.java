package com.dooapp.gaedo.blueprints;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;

import com.dooapp.gaedo.finders.FinderCrudService;
import com.dooapp.gaedo.test.beans.Post;
import com.dooapp.gaedo.test.beans.State;
import com.dooapp.gaedo.test.beans.Tag;
import com.dooapp.gaedo.test.beans.User;
import com.dooapp.gaedo.test.beans.UserInformer;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.Vertex;

import static com.dooapp.gaedo.blueprints.TestUtils.ABOUT_ID;
import static com.dooapp.gaedo.blueprints.TestUtils.TAG_TEXT;
import static com.dooapp.gaedo.blueprints.TestUtils.USER_LOGIN;
import static com.dooapp.gaedo.blueprints.TestUtils.USER_PASSWORD;

/**
 * Base class for post tests, allows some separation of concerns
 * @author ndx
 *
 */
public abstract class AbstractGraphPostSubClassTest extends AbstractGraphTest {
	private static final Logger logger = Logger.getLogger(AbstractGraphPostSubClassTest.class.getName());

	protected User author;
	protected Post post1;
	protected Post post2;
	protected Post post3;
	protected Tag tag1;

	public AbstractGraphPostSubClassTest(AbstractGraphEnvironment<?> environment) {
		super(environment);
	}

	public AbstractGraphPostSubClassTest(GraphProvider graphProvider) {
		super(graphProvider);
	}

	@Before
	public void loadService() throws Exception {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, TestUtils.TEST_SEPARATOR+"Starting loading services\n"+TestUtils.TEST_SEPARATOR);
		}
		super.loadService();

		// create some objects
		author = new User().withId(1).withLogin(USER_LOGIN).withPassword(USER_PASSWORD);
		author.about = new Post(ABOUT_ID, "a message about that user", 5, State.PUBLIC, author);
		author = getUserService().create(author);
		tag1 = getTagService().create(new Tag(1, TAG_TEXT));
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, TestUtils.TEST_SEPARATOR+"Loaded all services and test data\n"+TestUtils.TEST_SEPARATOR);
		}
	}

	protected Vertex findVertexIn(FinderCrudService<User, UserInformer> userService, User author) {
//		if(environment.getGraph() instanceof TransactionalGraph) {
//			((TransactionalGraph) environment.getGraph()).startTransaction();
//		}
		try {
			return ((AbstractBluePrintsBackedFinderService) getUserService()).getIdVertexFor(author, false);
		} finally {
			if(environment.getGraph() instanceof TransactionalGraph) {
				((TransactionalGraph) environment.getGraph()).stopTransaction(Conclusion.SUCCESS);
			}
		}
	}

	public static Map<String, String> theseMappings(String...strings) {
		Map<String, String> returned = new TreeMap<String, String>();
		for (int i = 0; i < strings.length; i++) {
			if(i+1<strings.length) {
				returned.put(strings[i++], strings[i]);
			}
		}
		return returned;
	}

}
