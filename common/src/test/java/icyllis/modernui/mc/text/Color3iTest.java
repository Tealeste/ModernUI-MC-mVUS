package icyllis.modernui.mc.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings("deprecation")
class Color3iTest {

    @Test
    void constructorFromRgbPacksColor() {
        Color3i color = new Color3i(0x11, 0x22, 0x33);
        assertEquals(0x11, color.getRed());
        assertEquals(0x22, color.getGreen());
        assertEquals(0x33, color.getBlue());
        assertEquals(0x112233, color.getColor());
    }

    @Test
    void constructorFromPackedColorExtractsRgb() {
        Color3i color = new Color3i(0xAABBCC);
        assertEquals(0xAA, color.getRed());
        assertEquals(0xBB, color.getGreen());
        assertEquals(0xCC, color.getBlue());
        assertEquals(0xAABBCC, color.getColor());
    }

    @Test
    void fromFormattingCodeReturnsExpectedConstants() {
        assertSame(Color3i.BLACK, Color3i.fromFormattingCode(0));
        assertSame(Color3i.DARK_BLUE, Color3i.fromFormattingCode(1));
        assertSame(Color3i.WHITE, Color3i.fromFormattingCode(15));
    }

    @Test
    void fromFormattingCodeOutOfRangeFallsBackToWhite() {
        assertSame(Color3i.WHITE, Color3i.fromFormattingCode(-1));
        assertSame(Color3i.WHITE, Color3i.fromFormattingCode(16));
        assertSame(Color3i.WHITE, Color3i.fromFormattingCode(Integer.MIN_VALUE));
        assertSame(Color3i.WHITE, Color3i.fromFormattingCode(Integer.MAX_VALUE));
    }

    @Test
    void componentHelpersReturnNormalizedFloats() {
        int color = 0x112233;
        assertEquals(0x11 / 255.0f, Color3i.getRedFrom(color), 0.000001f);
        assertEquals(0x22 / 255.0f, Color3i.getGreenFrom(color), 0.000001f);
        assertEquals(0x33 / 255.0f, Color3i.getBlueFrom(color), 0.000001f);
    }
}

