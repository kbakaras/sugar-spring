package ru.kbakaras.sugar.spring;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class PackageResolver {

    private ClassLoader classLoader;

    private ResourcePatternResolver resourcePatternResolver;
    private MetadataReaderFactory metadataReaderFactory;


    public PackageResolver() {
        this(null);
    }

    @SuppressWarnings("WeakerAccess")
    public PackageResolver(ClassLoader classLoader) {

        this.classLoader = classLoader != null ? classLoader : getClass().getClassLoader();

        resourcePatternResolver = new PathMatchingResourcePatternResolver(classLoader);
        metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

    }


    public void forEach(String basePackage, Class annotationClass,
                            BiConsumer<Class, Map<String, Object>> consumer) {

        String type = annotationClass.getName();

        try {
            Set<Class> included = new HashSet<>();
            for (Resource resource: resourcePatternResolver.getResources(searchPath(basePackage))) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    AnnotationMetadata am = metadataReader.getAnnotationMetadata();

                    if (am.hasAnnotation(type)) {
                        Class clazz = Class.forName(
                                metadataReader.getClassMetadata().getClassName(),
                                true, classLoader
                        );

                        if (!included.contains(clazz)) {
                            included.add(clazz);
                            consumer.accept(clazz, am.getAnnotationAttributes(type));
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

	private static String searchPath(String basePackage) {
        return ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(
                        SystemPropertyUtils.resolvePlaceholders(basePackage)) +
                "/**/*.class";
    }

}