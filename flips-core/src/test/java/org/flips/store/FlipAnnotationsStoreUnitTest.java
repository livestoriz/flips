package org.flips.store;

import org.flips.annotation.Flips;
import org.flips.model.FlipConditionEvaluator;
import org.flips.model.FlipConditionEvaluatorFactory;
import org.flips.processor.FlipAnnotationProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FlipAnnotationsStoreUnitTest {

    public static class FlipAnnotationTestClient {
        public  void method1(){
        }
        private void method2(){
        }
    }

    @InjectMocks
    private FlipAnnotationsStore flipAnnotationsStore;

    @Mock
    private ApplicationContext   applicationContext;

    @Mock
    private FlipAnnotationProcessor flipAnnotationProcessor;

    @Mock
    private FlipConditionEvaluatorFactory flipConditionEvaluatorFactory;

    @Test
    public void shouldNotStoreFlipAnnotationsGivenNoBeansWereAnnotatedWithFlipsAnnotation(){
        when(applicationContext.getBeansWithAnnotation(Flips.class)).thenReturn(Collections.EMPTY_MAP);

        flipAnnotationsStore.buildFlipAnnotationsStore();

        assertEquals(0, flipAnnotationsStore.getTotalMethodsCached());
        assertEquals(0, flipAnnotationsStore.allMethodsCached().size());
        verify(applicationContext).getBeansWithAnnotation(Flips.class);
        verify(flipAnnotationProcessor, never()).getFlipConditionEvaluator(any(Method.class));
    }

    @Test
    public void shouldNotStoreFlipAnnotationsGivenEmptyFlipConditionEvaluator() throws Exception {
        Map<String, Object> flipComponents = new HashMap<String, Object>(){{
            put("featureFlipAnnotationClient", new FlipAnnotationTestClient());
        }};

        Method method                                      = FlipAnnotationTestClient.class.getMethod("method1");
        FlipConditionEvaluator emptyFlipConditionEvaluator = mock(FlipConditionEvaluator.class);

        when(applicationContext.getBeansWithAnnotation(Flips.class)).thenReturn(flipComponents);
        when(flipAnnotationProcessor.getFlipConditionEvaluator(method)).thenReturn(emptyFlipConditionEvaluator);
        when(emptyFlipConditionEvaluator.isEmpty()).thenReturn(true);

        flipAnnotationsStore.buildFlipAnnotationsStore();

        assertEquals(0, flipAnnotationsStore.getTotalMethodsCached());
        verify(applicationContext).getBeansWithAnnotation(Flips.class);
        verify(flipAnnotationProcessor).getFlipConditionEvaluator(method);
        verify(emptyFlipConditionEvaluator).isEmpty();
    }

    @Test
    public void shouldStoreFlipAnnotationsGivenBeansWereAnnotatedWithFlipsAnnotationsAndMethodsWereAccessibleWithNonEmptyFlipConditionEvaluator() throws Exception {
        Map<String, Object> flipComponents = new HashMap<String, Object>(){{
            put("featureFlipAnnotationClient", new FlipAnnotationTestClient());
        }};

        Method method                                 = FlipAnnotationTestClient.class.getMethod("method1");
        FlipConditionEvaluator flipConditionEvaluator = mock(FlipConditionEvaluator.class);

        when(applicationContext.getBeansWithAnnotation(Flips.class)).thenReturn(flipComponents);
        when(flipAnnotationProcessor.getFlipConditionEvaluator(method)).thenReturn(flipConditionEvaluator);
        when(flipConditionEvaluator.isEmpty()).thenReturn(false);

        flipAnnotationsStore.buildFlipAnnotationsStore();

        assertEquals(1, flipAnnotationsStore.getTotalMethodsCached());
        assertEquals(1, flipAnnotationsStore.allMethodsCached().size());
        verify(applicationContext).getBeansWithAnnotation(Flips.class);
        verify(flipAnnotationProcessor).getFlipConditionEvaluator(method);
        verify(flipConditionEvaluator).isEmpty();
    }

    @Test
    public void shouldReturnFeatureEnabledGivenFlipConditionEvaluatorReturnsTrue() throws Exception{
        Method method                           = FlipAnnotationTestClient.class.getMethod("method1");
        FlipConditionEvaluator flipConditionEvaluator = mock(FlipConditionEvaluator.class);
        Map<Method, FlipConditionEvaluator> store   = new HashMap<Method, FlipConditionEvaluator>(){{
            put(method, flipConditionEvaluator);
        }};

        ReflectionTestUtils.setField(flipAnnotationsStore, "store", store);

        when(flipConditionEvaluator.evaluate()).thenReturn(true);
        boolean featureEnabled = flipAnnotationsStore.isFeatureEnabled(method);

        assertEquals(true, featureEnabled);
        verify(flipConditionEvaluator).evaluate();
    }

    @Test
    public void shouldReturnFeatureDisabledGivenFlipConditionEvaluatorReturnsFalse() throws Exception{
        Method method                           = FlipAnnotationTestClient.class.getMethod("method1");
        FlipConditionEvaluator flipConditionEvaluator = mock(FlipConditionEvaluator.class);
        Map<Method, FlipConditionEvaluator> store   = new HashMap<Method, FlipConditionEvaluator>(){{
            put(method, flipConditionEvaluator);
        }};

        ReflectionTestUtils.setField(flipAnnotationsStore, "store", store);

        when(flipConditionEvaluator.evaluate()).thenReturn(false);
        boolean featureEnabled = flipAnnotationsStore.isFeatureEnabled(method);

        assertEquals(false, featureEnabled);
        verify(flipConditionEvaluator).evaluate();
    }

    @Test
    public void shouldReturnFeatureEnabledGivenMethodIsNotCached() throws Exception{
        Method method                           = FlipAnnotationTestClient.class.getMethod("method1");
        FlipConditionEvaluator flipConditionEvaluator = mock(FlipConditionEvaluator.class);
        Map<Method, FlipConditionEvaluator> store   = new HashMap<Method, FlipConditionEvaluator>(){{
            put(null, flipConditionEvaluator);
        }};
        ReflectionTestUtils.setField(flipAnnotationsStore, "store", store);

        when(flipConditionEvaluatorFactory.getEmptyFlipConditionEvaluator()).thenReturn(flipConditionEvaluator);
        when(flipConditionEvaluator.evaluate()).thenReturn(true);
        boolean featureEnabled = flipAnnotationsStore.isFeatureEnabled(method);

        assertEquals(true, featureEnabled);
        verify(flipConditionEvaluator).evaluate();
    }
}
