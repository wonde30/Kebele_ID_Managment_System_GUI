package ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the createEditableField() method in KebeleIDSystem.
 * Tests verify that the method exists and has the correct signature.
 * Full integration testing will be done when the method is used in the approval dialog.
 */
class CreateEditableFieldTest {

    @Test
    void testCreateEditableField_MethodExists() throws Exception {
        // Verify the method exists with correct signature
        Method method = KebeleIDSystem.class.getDeclaredMethod("createEditableField", String.class);
        assertNotNull(method, "createEditableField method should exist");
        assertEquals(JTextField.class, method.getReturnType(), 
            "Method should return JTextField");
        
        // Verify method is private as per design
        assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()),
            "Method should be private");
    }

    @Test
    void testCreateEditableField_AcceptsStringParameter() throws Exception {
        Method method = KebeleIDSystem.class.getDeclaredMethod("createEditableField", String.class);
        Class<?>[] paramTypes = method.getParameterTypes();
        
        assertEquals(1, paramTypes.length, "Method should accept exactly one parameter");
        assertEquals(String.class, paramTypes[0], "Parameter should be of type String");
    }
}
