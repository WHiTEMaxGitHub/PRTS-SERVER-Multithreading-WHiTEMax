/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.izzel.arclight.common.paper;

import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperChunkLoadsTest {

    @Test
    void aMissingFutureCompletesWithNull() {
        assertNull(PaperChunkLoads.bukkitChunk(null).join());
    }

    @Test
    void aFailedFutureCompletesWithNull() {
        CompletableFuture<ChunkResult<ChunkAccess>> failed =
            CompletableFuture.completedFuture(ChunkResult.error("absent"));
        assertNull(PaperChunkLoads.bukkitChunk(failed).join());
    }

    @Test
    void theGenerateFlagPassesThrough() {
        assertFalse(PaperChunkLoads.generationFlag(false));
        assertTrue(PaperChunkLoads.generationFlag(true));
    }
}
