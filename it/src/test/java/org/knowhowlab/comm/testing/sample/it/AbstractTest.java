package org.knowhowlab.comm.testing.sample.it;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.knowhowlab.osgi.testing.assertions.OSGiAssert;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author dpishchukhin
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public abstract class AbstractTest {
    /**
     * Injected BundleContext
     */
    @Inject
    protected BundleContext bc;

    @Before
    public void init() {
        OSGiAssert.setDefaultBundleContext(bc);
    }

    /**
     * Runner config
     *
     * @return config
     */
    protected static Option[] baseConfiguration(Option... extraOptions) {
        Option[] options = options(
                junitBundles(),
                // list of bundles that should be installed
                mavenBundle().groupId("org.knowhowlab.osgi").artifactId("org.knowhowlab.osgi.testing.all").version("1.3.0"),

                systemProperty("project.version").value(System.getProperty("project.version")),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN")
        );
        if (extraOptions != null) {
            options = combine(options, extraOptions);
        }
        return options;
    }

}
