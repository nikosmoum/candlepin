/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.policy.js;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * JsRunner - Responsible for running the javascript rules methods in all
 * namespaces.
 * Used by the various "Rules" classes.
 */
public class JsRunner {

    private static Logger log = LoggerFactory.getLogger(JsRunner.class);

    private Object rulesNameSpace;
    private String namespace;
    private Scriptable scope;

    private boolean initialized = false;

    public JsRunner(Scriptable scope) {
        this.scope = scope;
    }

    /**
     * initialize the javascript rules for the provided namespace. you must run
     * this
     * before trying to run a javascript rule or method.
     *
     * @param namespace the javascript rules namespace containing the rules type
     *        you want
     */
    public void init(String namespace) {
        this.namespace = namespace;

        if (!initialized) {

            Context context = Context.enter();
            try {
                Object func = ScriptableObject.getProperty(scope, namespace);
                this.rulesNameSpace = unwrapReturnValue(((Function) func)
                    .call(context, scope, scope, Context.emptyArgs));

                this.initialized = true;
            }
            catch (RhinoException ex) {
                this.initialized = false;
                throw new RuleParseException(ex);
            }
            finally {
                Context.exit();
            }
        }
    }

    public void reinitTo(String namespace) {
        initialized = false;
        init(namespace);
    }

    Object unwrapReturnValue(Object result) {
        if (result instanceof Wrapper) {
            result = ((Wrapper) result).unwrap();
        }

        return result instanceof Undefined ? null : result;
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeMethod(String method)
        throws NoSuchMethodException, RhinoException {
        Scriptable localScope = Context.toObject(this.rulesNameSpace, scope);
        Object func = ScriptableObject.getProperty(localScope, method);
        if (!(func instanceof Function)) {
            throw new NoSuchMethodException("no such javascript method: " + method);
        }
        Context context = Context.enter();
        try {
            return (T) unwrapReturnValue(((Function) func).call(context, scope,
                localScope, Context.emptyArgs));
        }
        finally {
            Context.exit();
        }
    }


    @SuppressWarnings("unchecked")
    public <T> T invokeMethodGraalVM(String methodName, JsContext contextArgs, Class<T> clazz) {
        JsonJsContext jsonContextArgs = (JsonJsContext) contextArgs;
        Logger logger = jsonContextArgs.getLogger();
        RulesObjectMapper mapper = jsonContextArgs.getRulesObjectMapper();
        String jsonArgs = mapper.toJsonString(jsonContextArgs.contextArgs);

        try {
            // Create GraalVM context
            org.graalvm.polyglot.Context context = org.graalvm.polyglot.Context
                .newBuilder("js")
                .allowHostAccess(HostAccess.ALL) // So that JavaScript code can call Java methods (in our
                                                 // case, the logger, which is passed in un-serialized).
                .option("inspect", "localhost:8181") // enable Chrome Dev Tools debugger option
                .option("inspect.Path", "my-debug-path")
                .option("inspect.Secure", "false")
                .build();

            // The debugger is accessible through Chrome on this URL: chrome-devtools://devtools/bundled/js_app.html?ws=127.0.0.1:8181/my-debug-path

            // Load & parse the rules file
            URL rulesfile = this.getClass().getClassLoader().getResource("rules/rules.js");
            context.eval(Source.newBuilder("js", rulesfile).build());

            // Get all the top-level bindings of the file (methods, namespaces, etc.)
            Value allBindings = context.getBindings("js");

            // Get the namespace we're calling into
            Value namespaceValue = allBindings.getMember(namespace).execute();

            // Set the input as attributes
            allBindings.putMember("json_context", jsonArgs);
            allBindings.putMember("log", logger);

            // Call the method
            Value result = namespaceValue.getMember(methodName).execute();
            if (clazz == null) {
                return (T) result.asString();
            }
            else {
                return result.as(clazz);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeMethod(String method, JsContext context)
        throws NoSuchMethodException, RhinoException {
        if (true) {
            return (T) this.invokeMethodGraalVM(method, context, null);
        }
        context.applyTo(scope);
        return (T) invokeMethod(method);
    }

    public <T> T invokeRule(String ruleName) {
        log.debug("Running rule: {} in namespace: {}", ruleName, namespace);

        T returner = null;
        try {
            returner = this.invokeMethod(ruleName);
        }
        catch (NoSuchMethodException ex) {
            log.info("No rule found: {} in namespace: {}", ruleName, namespace);
        }
        catch (RhinoException ex) {
            throw new RuleExecutionException(ex);
        }
        return returner;
    }

    public <T> T invokeRule(String ruleName, JsContext context) {
        context.applyTo(scope);
        return invokeRule(ruleName);
    }

    public <T extends Object> T runJsFunction(Class<T> clazz, String function,
        JsContext context) {
        if (true) {
            return this.invokeMethodGraalVM(function, context, clazz);
        }
        T returner = null;
        try {
            returner = invokeMethod(function, context);
        }
        catch (NoSuchMethodException e) {
            log.warn("No javascript method found: {}", function);
        }
        catch (RhinoException e) {
            throw new RuleExecutionException(e);
        }
        return returner;
    }
}
