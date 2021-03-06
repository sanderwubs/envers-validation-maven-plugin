package com.github.zeger_tak.enversvalidationplugin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Parameterized
{
	@Nonnull String name() default "{index}";

	@Nonnull String uniqueIdentifier() default "{index}";
}