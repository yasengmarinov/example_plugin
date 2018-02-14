package com.ontotext.trree.plugin;

import com.ontotext.trree.sdk.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.util.iterators.SingletonIterator;

import java.util.Date;
import java.util.Iterator;

public class ExamplePlugin extends PluginBase implements PatternInterpreter, Preprocessor, Postprocessor, Configurable {

	private static final String PREDICATE_PARAM = "predicate-uri"; // parameter name
	private static final String DEFAULT_PREDICATE = "http://example.com/time"; // parameter default value

	private IRI predicate;
	private long predicateId;

	// Service interface methods
	@Override
	public String getName() {
		return "example";
	}

	// Plugin interface methods
	@Override
	public void initialize(InitReason reason) {
		String predicateName = getOptions().getParameter(PREDICATE_PARAM, DEFAULT_PREDICATE);
		predicate = SimpleValueFactory.getInstance().createIRI(predicateName);
		predicateId = getEntities().put(predicate, Entities.Scope.SYSTEM);

		getLogger().info("Example plugin initialized!");
	}

	// PatternInterpreter interface methods
	@Override
	public StatementIterator interpret(long subject, long predicate, long object, long context,
									   Statements statements, Entities entities, RequestContext requestContext) {
		// ignore patterns with predicate different than the one we recognize
		if (predicate != predicateId)
			return null;

		// create the date/time literal
		// here it is important to create the literal in the entities instance of the request and NOT in getEntities()
		long literalId = createDateTimeLiteral(entities);

		// return a StatementIterator with a single statement to be iterated
		return StatementIterator.create(subject, predicate, literalId, 0);
	}

	@Override
	public double estimate(long subject, long predicate, long object, long context,
						   Statements statements, Entities entities, RequestContext requestContext) {
		// We always return a single statement
		return 1;
	}

	// Preprocessor interface methods
	@Override
	public RequestContext preprocess(Request request) {
		if (request instanceof QueryRequest) {
			QueryRequest queryRequest = (QueryRequest) request;
			Dataset dataset = queryRequest.getDataset();

			// check if the predicate is included in the default graph
			if ((dataset != null && dataset.getDefaultGraphs().contains(predicate))) {
				// create a date/time literal
				long literalId = createDateTimeLiteral(getEntities());
				Value literal = getEntities().get(literalId);

				// prepare a binding set with all projected variables set to the date/time literal value
				MapBindingSet result = new MapBindingSet();
				for (String bindingName : queryRequest.getTupleExpr().getBindingNames()) {
					result.addBinding(bindingName, literal);
				}
				return new Context(result);
			}
		}
		return null;
	}

	// Postprocessor interface methods
	@Override
	public boolean shouldPostprocess(RequestContext requestContext) {
		// postprocess only if we have crated RequestContext in the preprocess phase
		return requestContext != null;
	}

	@Override
	public BindingSet postprocess(BindingSet bindingSet, RequestContext requestContext) {
		// filter all results - we will add the result we want in the flush() method
		return null;
	}

	@Override
	public Iterator<BindingSet> flush(RequestContext requestContext) {
		// return the binding set we have created in the preprocess phase
		BindingSet result = ((Context) requestContext).getResult();
		return new SingletonIterator<>(result);
	}

	// Configurable interface methods
	@Override
	public String[] getParameters() {
		// GraphDB should expect our parameter
		return new String[] { PREDICATE_PARAM };
	}

	private long createDateTimeLiteral(Entities entities) {
		// create an IRI and add it to the given entity pool
		Value literal = SimpleValueFactory.getInstance().createLiteral(new Date().toString());
		return entities.put(literal, Entities.Scope.REQUEST);
	}

	/**
	 * Context where we can store data during the processing phase
	 */
	private static class Context implements RequestContext {
		private Request theRequest;
		private BindingSet theResult;

		public Context(BindingSet result) {
			theResult = result;
		}

		@Override
		public Request getRequest() {
			return theRequest;
		}

		@Override
		public void setRequest(Request request) {
			theRequest = request;
		}

		public BindingSet getResult() {
			return theResult;
		}
	}
}