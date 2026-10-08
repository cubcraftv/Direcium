package dev.cubcraftv.direcium.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class DireciumPushBlockTest {
	@Test
	void writesTheLayoutTheShaderBlockExpects() {
		ByteBuffer out = DireciumPushBlock.allocate();
		DireciumPushBlock.encode(out, 1.5f, -2.25f, 3.0f, 1234, 42);

		assertEquals(0, out.position());
		assertEquals(20, out.limit());
		assertEquals(1.5f, out.getFloat(0));
		assertEquals(-2.25f, out.getFloat(4));
		assertEquals(3.0f, out.getFloat(8));
		assertEquals(1234, out.getInt(12));
		assertEquals(42, out.getInt(16));
	}

	@Test
	void sizeMatchesSodiumsPushConstantRange() {
		// DrawContext.PUSH_CONSTANT_RANGE in Sodium 26.2.
		assertEquals(20, DireciumPushBlock.SIZE_BYTES);
	}

	@Test
	void reencodingOverwritesAndResetsTheBuffer() {
		ByteBuffer out = DireciumPushBlock.allocate();
		DireciumPushBlock.encode(out, 1f, 2f, 3f, 4, 5);
		out.position(20);
		DireciumPushBlock.encode(out, 9f, 8f, 7f, 6, 5);

		assertEquals(0, out.position());
		assertEquals(20, out.remaining());
		assertEquals(9f, out.getFloat(0));
		assertEquals(6, out.getInt(12));
	}

	@Test
	void regionIdUsesTheFullUnsignedRangeAsRawBits() {
		ByteBuffer out = DireciumPushBlock.allocate();
		DireciumPushBlock.encode(out, 0f, 0f, 0f, 0, -1);
		assertEquals(-1, out.getInt(16));
	}

	@Test
	void rejectsTooSmallOrWrongOrderBuffers() {
		assertThrows(IllegalArgumentException.class,
			() -> DireciumPushBlock.encode(ByteBuffer.allocate(16).order(ByteOrder.nativeOrder()), 0f, 0f, 0f, 0, 0));

		ByteOrder other = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
		assertThrows(IllegalArgumentException.class,
			() -> DireciumPushBlock.encode(ByteBuffer.allocate(20).order(other), 0f, 0f, 0f, 0, 0));
	}
}
