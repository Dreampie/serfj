package com.elpaso.serfj.finders;

import com.elpaso.serfj.util.UrlUtils;

import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Javadoc
 * @author: Eduardo Yáñez
 * Date: 08-may-2009
 */
public class ResourceFinder {
    private static final Logger logger = LoggerFactory.getLogger(ResourceFinder.class);
    
    private static final String MODEL_STYLE = "model";
    private static final String FUNCTIONAL_BY_MODEL_STYLE = "functional_by_model";
    private static final String FUNCTIONAL_STYLE = "functional";
    private static final String OFF_OPTION = "OFF";
    
    /**
     * Default package for classes used when some class is not found and there is a SERFJ implementation
     * for something. For example, a XmlSerializer, a JsonSerializer, and so on.
     */
    protected static final String DEFAULT_PACKAGE = "com.elpaso.serfj";

    protected String mainPackage;
    private String alias;
    protected String prefix;
    private String suffix;
    private String style;
    protected UrlUtils utils = UrlUtils.getInstance();

    ResourceFinder(String mainPackage, String alias, String prefix, String suffix, String style) {
        this(mainPackage, alias, suffix, style);
        this.prefix = prefix.toLowerCase();
    }

    public ResourceFinder(String mainPackage, String alias, String suffix, String style) {
        if (logger.isDebugEnabled()) {
            Object[] params = new Object[] {mainPackage, alias, suffix, style};
            logger.debug("MainPackage [{}], Alias [{}], Suffix [{}], Style [{}]", params);
        }
        this.mainPackage = mainPackage;
        this.alias = alias;
        if (!OFF_OPTION.equals(suffix)) {
            this.suffix = suffix;
        }
        this.style = style;
    }

    public String findResource(String model) {
        if (model == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Searching a default resource for model [{}]", model);
            }
            return this.defaultResource(model);
        }

        String clazz;
        if (FUNCTIONAL_STYLE.equals(style)) {
            clazz = findByFunction(model);
        } else if (FUNCTIONAL_BY_MODEL_STYLE.equals(style)) {
            clazz = findByFunctionAndModel(model);
        } else if (MODEL_STYLE.equals(style)) {
            clazz = findByModel(model);
        } else {
            // Style wasn't defined, so we search the class first as Functional style,
            // then as Functional by Resource style, and last by Resource
            clazz = findByFunction(model);
            if (clazz == null) {
                clazz = findByFunctionAndModel(model);
                if (clazz == null) {
                    clazz = findByModel(model);
                }
            }
        }
        if (clazz == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Searching a default resource for model [{}]", model);
            }
            clazz = this.defaultResource(model);
        }
        return clazz;
    }

    /**
     * Gets the default resource in case finder can't find an implementation.
     *
     * @param model - Model.
     * @return always returns null. If a finder know there are default resources, it
     * must override this method.
     */
    protected String defaultResource(String model) {
        return null;
    }

    private Boolean existsClass(String clazz) {
        Boolean exists = true;
        try {
            Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Class {} doesn't exists in the Classpath", clazz);
            }
            exists = false;
        }
        return exists;
    }

    protected String findByFunction(String model) {
        String clazz = MessageFormat.format("{0}.{1}.{2}", mainPackage, alias, this.makeClassName(model));
        if (logger.isDebugEnabled()) {
            logger.debug("Searching resource [{}] by FUNCTIONAL style", clazz);
        }
        if (!existsClass(clazz)) {
            return null;
        }
        return clazz;
    }

    protected String findByFunctionAndModel(String model) {
        String clazz = MessageFormat.format("{0}.{1}.{2}.{3}", mainPackage, utils.singularize(model),
                alias, this.makeClassName(model));
        if (logger.isDebugEnabled()) {
            logger.debug("Searching resource [{}] by FUNCTIONAL BY MODEL style", clazz);
        }
        if (!existsClass(clazz)) {
            return null;
        }
        return clazz;
    }

    protected String findByModel(String model) {
        String clazz = MessageFormat.format("{0}.{1}.{2}", mainPackage, utils.singularize(model),
                this.makeClassName(model));
        if (logger.isDebugEnabled()) {
            logger.debug("Searching resource [{}] by MODEL style", clazz);
        }
        if (!existsClass(clazz)) {
            return null;
        }
        return clazz;
    }

    protected String makeClassName(String model) {
        String clazz = MessageFormat.format("{0}{1}{2}", utils.capitalize(prefix),
                utils.capitalize(utils.singularize(model)), utils.capitalize(suffix));
        return clazz;
    }
}