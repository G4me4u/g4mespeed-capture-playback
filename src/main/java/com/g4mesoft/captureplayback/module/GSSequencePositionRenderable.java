package com.g4mesoft.captureplayback.module;

import java.util.Set;
import java.util.TreeSet;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.renderer.GSERenderPhase;
import com.g4mesoft.renderer.GSIRenderable3D;
import com.g4mesoft.renderer.GSIRenderer3D;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class GSSequencePositionRenderable implements GSIRenderable3D {

	private static final int COLOR_ALPHA = 0x80;
	private static final float UNIT_SURFACE_OFFSET = 2.0e-4f;
	private static final float MINIMUM_SURFACE_OFFSET = 1.0e-3f;
	
	private final GSCapturePlaybackModule module;
	private final GSSequence sequence;
	
	public GSSequencePositionRenderable(GSCapturePlaybackModule module, GSSequence sequence) {
		this.module = module;
		this.sequence = sequence;
	}
	
	@Override
	public void render(GSIRenderer3D renderer) {
		switch(module.cChannelRenderingType.getValue()) {
		case GSCapturePlaybackModule.RENDERING_DEPTH:
			renderCubes(renderer);
			break;
		case GSCapturePlaybackModule.RENDERING_NO_DEPTH:
			RenderSystem.disableDepthTest();
			renderCubes(renderer);
			RenderSystem.enableDepthTest();
			break;
		case GSCapturePlaybackModule.RENDERING_DISABLED:
		default:
			// Skip rendering in this case
			break;
		}
	}
	
	private void renderCubes(GSIRenderer3D renderer) {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
		float viewDistance = client.gameRenderer.getViewDistance();
		
		Set<GSCubeEntry> cubes = new TreeSet<>();
		for (GSChannel channel : sequence.getChannels()) {
			GSChannelInfo info = channel.getInfo();
			
			for (BlockPos position : info.getPositions()) {
				// Distance measured relative to center of block
				double dx = position.getX() - cameraPos.getX() + 0.5;
				double dy = position.getY() - cameraPos.getY() + 0.5;
				double dz = position.getZ() - cameraPos.getZ() + 0.5;
				
				float dist = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (dist <= viewDistance)
					cubes.add(new GSCubeEntry(position, info.getColor(), dist, cubes.size()));
			}
		}
		
		renderer.build(GSIRenderer3D.QUADS, VertexFormats.POSITION_COLOR);

		for (GSCubeEntry cube : cubes) {
			// Render cube relative to camera position
			float rx = (float)(cube.position.getX() - cameraPos.getX());
			float ry = (float)(cube.position.getY() - cameraPos.getY());
			float rz = (float)(cube.position.getZ() - cameraPos.getZ());
			int color = (cube.color & 0x00FFFFFF) | (COLOR_ALPHA << 24);
			
			// Offset edges to fix issues with z-fighting
			float offset = Math.max(MINIMUM_SURFACE_OFFSET, cube.dist * UNIT_SURFACE_OFFSET);

			renderer.fillCuboid(rx - offset,
			                    ry - offset,
			                    rz - offset,
			                    rx + 1.0f + offset,
			                    ry + 1.0f + offset,
			                    rz + 1.0f + offset,
			                    color);
		}
		
		renderer.finish();
	}
	
	@Override
	public GSERenderPhase getRenderPhase() {
		return GSERenderPhase.TRANSPARENT_LAST;
	}
	
	private class GSCubeEntry implements Comparable<GSCubeEntry> {
		
		private final BlockPos position;
		private final int color;
		private final float dist;
		private final int order;
		
		public GSCubeEntry(BlockPos position, int color, float distSqr, int order) {
			this.position = position;
			this.color = color;
			this.dist = distSqr;
			this.order = order;
		}
		
		@Override
		public int compareTo(GSCubeEntry other) {
			// Sort back to front (decreasing distance).
			int cmp = Float.compare(other.dist, dist);
			return (cmp != 0) ? cmp : Integer.compare(order, other.order);
		}
	}
}
