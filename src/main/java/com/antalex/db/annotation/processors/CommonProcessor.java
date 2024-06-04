package com.antalex.db.annotation.processors;

import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.ShardEntity;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes({"com.antalex.db.annotation.ShardEntity", "com.antalex.db.annotation.DomainEntity"})
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CommonProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : set) {
            for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                try {
                    if (annotatedElement.getAnnotation(ShardEntity.class) != null) {
                        EntityClassBuilder.createInterceptorClass(annotatedElement, processingEnv);
                        EntityClassBuilder.createRepositoryClass(annotatedElement, processingEnv);
                    }
                    if (annotatedElement.getAnnotation(DomainEntity.class) != null) {
                        DomainClassBuilder.createInterceptorClass(annotatedElement, processingEnv);
                        DomainClassBuilder.createMapperClass(annotatedElement, processingEnv);
                    }
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        }
        return true;
    }
}
