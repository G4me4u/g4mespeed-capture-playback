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
	private static final float SURFACE_OFFSET = 1.0e-3f;
	
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
		
		Set<GSCubeEntry> cubes = new TreeSet<>();
		for (GSChannel channel : sequence.getChannels()) {
			BlockPos pos = channel.getInfo().getPos();
			
			float dx = (float)(pos.getX() + 0.5 - cameraPos.x);
			float dy = (float)(pos.getY() + 0.5 - cameraPos.y);
			float dz = (float)(pos.getZ() + 0.5 - cameraPos.z);
			float distSqr = dx * dx + dy * dy + dz * dz;
			
			cubes.add(new GSCubeEntry(channel, distSqr, cubes.size()));
		}
		
		renderer.build(GSIRenderer3D.QUADS, VertexFormats.POSITION_COLOR);

		for (GSCubeEntry cube : cubes) {
			GSChannelInfo channelInfo = cube.channel.getInfo();
			BlockPos pos = channelInfo.getPos();
			
			float x0 = pos.getX() - SURFACE_OFFSET;
			float y0 = pos.getY() - SURFACE_OFFSET;
			float z0 = pos.getZ() - SURFACE_OFFSET;
			
			float x1 = pos.getX() + 1.0f + SURFACE_OFFSET;
			float y1 = pos.getY() + 1.0f + SURFACE_OFFSET;
			float z1 = pos.getZ() + 1.0f + SURFACE_OFFSET;

			int color = (channelInfo.getColor() & 0x00FFFFFF) | (COLOR_ALPHA << 24);
			
			renderer.fillCuboid(x0, y0, z0, x1, y1, z1, color);
		}
		
		renderer.finish();
	}
	
	@Override
	public GSERenderPhase getRenderPhase() {
		return GSERenderPhase.TRANSPARENT_LAST;
	}
	
	private class GSCubeEntry implements Comparable<GSCubeEntry> {
		
		private final GSChannel channel;

		private final float distSqr;
		private final int order;
		
		public GSCubeEntry(GSChannel channel, float distSqr, int order) {
			this.channel = channel;

			this.distSqr = distSqr;
			this.order = order;
		}
		
		@Override
		public int compareTo(GSCubeEntry other) {
			// Sort back to front (decreasing distance).
			int cmp = Float.compare(other.distSqr, distSqr);
			return (cmp != 0) ? cmp : Integer.compare(order, other.order);
		}
	}
}
