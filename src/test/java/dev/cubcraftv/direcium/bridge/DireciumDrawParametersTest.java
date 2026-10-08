package dev.cubcraftv.direcium.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.nio.IntBuffer;

final class DireciumDrawParametersTest {
	@Test
	void packsFirstIndexIndexCountBaseVertexPerDraw() {
		DireciumDrawParameters parameters = new DireciumDrawParameters(4);
		parameters.put(0, 6, 100, 12);
		parameters.put(1, 18, -4, 3000);

		IntBuffer view = parameters.view(2);
		assertEquals(0, view.position());
		assertEquals(6, view.limit());
		// firstIndex, indexCount, baseVertex
		assertEquals(12, view.get(0));
		assertEquals(6, view.get(1));
		assertEquals(100, view.get(2));
		assertEquals(3000, view.get(3));
		assertEquals(18, view.get(4));
		assertEquals(-4, view.get(5));
	}

	@Test
	void elementOffsetStaysInIndicesNotBytes() {
		DireciumDrawParameters parameters = new DireciumDrawParameters(1);
		parameters.put(0, 6, 0, 1_000_000L);
		assertEquals(1_000_000, parameters.view(1).get(0));
	}

	@Test
	void viewLimitFollowsTheDrawCountAndIsReused() {
		DireciumDrawParameters parameters = new DireciumDrawParameters(8);
		IntBuffer first = parameters.view(8);
		assertEquals(24, first.limit());
		IntBuffer second = parameters.view(3);
		assertEquals(9, second.limit());
		assertEquals(0, second.position());
	}

	@Test
	void sodiumsBatchCapacityFitsAndTheLastSlotIsUsable() {
		// ModelQuadFacing.COUNT (7) * RenderRegion.REGION_SIZE (8 * 4 * 8) + 1 in Sodium 26.2.
		int capacity = 7 * 256 + 1;
		DireciumDrawParameters parameters = new DireciumDrawParameters(capacity);
		parameters.put(capacity - 1, 6, 1, 2);
		assertEquals(capacity * 3, parameters.view(capacity).limit());
		assertEquals(2, parameters.view(capacity).get((capacity - 1) * 3));
	}

	@Test
	void rejectsOutOfRangeAccessAndOversizedOffsets() {
		DireciumDrawParameters parameters = new DireciumDrawParameters(2);
		assertThrows(IndexOutOfBoundsException.class, () -> parameters.put(2, 6, 0, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> parameters.put(-1, 6, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> parameters.view(3));
		assertThrows(ArithmeticException.class, () -> parameters.put(0, 6, 0, 1L << 33));
		assertThrows(IllegalArgumentException.class, () -> new DireciumDrawParameters(0));
	}
}
