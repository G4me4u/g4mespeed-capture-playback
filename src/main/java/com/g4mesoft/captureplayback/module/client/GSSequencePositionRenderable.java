package com.g4mesoft.captureplayback.module.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.ui.renderer.GSERenderPhase;
import com.g4mesoft.ui.renderer.GSIRenderable3D;
import com.g4mesoft.ui.renderer.GSIRenderer3D;
import com.g4mesoft.ui.util.GSColorUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class GSSequencePositionRenderable implements GSIRenderable3D {

	private static final int COLOR_ALPHA = 0x80;
	private static final float UNIT_SURFACE_OFFSET = 2.0e-4f;
	private static final float MINIMUM_SURFACE_OFFSET = 1.0e-3f;
	
	private static final float SELECTION_THICKNESS = 1.0f / 16.0f;
	private static final float SELECTION_NOTCH     = 4.0f / 16.0f;
	
	private static final float[] SELECTION_VERTICES = computeSelectionVertices(SELECTION_THICKNESS, SELECTION_NOTCH);
	
	private final GSCapturePlaybackClientModule module;
	private final GSClientAssetManager assetManager;
	
	public GSSequencePositionRenderable(GSCapturePlaybackClientModule module) {
		this.module = module;
		this.assetManager = module.getAssetManager();
	}
	
	@Override
	public void render(GSIRenderer3D renderer) {
		GSSession session = assetManager.getSession(GSESessionType.SEQUENCE);
		if (session != null) {
			GSSequence sequence = session.get(GSSession.SEQUENCE);
			
			switch(module.cChannelRenderingType.get()) {
			case GSCapturePlaybackClientModule.RENDERING_DEPTH:
				renderCubes(renderer, session, sequence);
				break;
			case GSCapturePlaybackClientModule.RENDERING_NO_DEPTH:
				RenderSystem.disableDepthTest();
				renderCubes(renderer, session, sequence);
				RenderSystem.enableDepthTest();
				break;
			case GSCapturePlaybackClientModule.RENDERING_DISABLED:
			default:
				// Skip rendering in this case
				break;
			}
		}
	}
	
	private void renderCubes(GSIRenderer3D renderer, GSSession session, GSSequence sequence) {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
		float viewDistance = client.gameRenderer.getViewDistance();
		
		UUID selectedChannelUUID = session.get(GSSession.SELECTED_CHANNEL);
		
		List<GSCubeEntry> cubes = new ArrayList<>();
		for (GSChannel channel : sequence.getChannels()) {
			GSChannelInfo info = channel.getInfo();
			boolean selected = Objects.equals(selectedChannelUUID, channel.getChannelUUID());
			
			for (BlockPos position : info.getPositions()) {
				// Distance measured relative to center of block
				double dx = position.getX() - cameraPos.getX() + 0.5;
				double dy = position.getY() - cameraPos.getY() + 0.5;
				double dz = position.getZ() - cameraPos.getZ() + 0.5;
				
				float dist = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (dist <= viewDistance)
					cubes.add(new GSCubeEntry(position, info.getColor(), dist, selected));
			}
		}
		// Efficiently uses Arrays.sort on backing array
		Collections.sort(cubes, (c0, c1) -> {
			// Sort back to front (decreasing distance).
			return Float.compare(c1.dist, c0.dist);
		});
		
		renderer.build(GSIRenderer3D.QUADS, VertexFormats.POSITION_COLOR);

		for (GSCubeEntry cube : cubes) {
			// Render cube relative to camera position
			float rx = (float)(cube.position.getX() - cameraPos.getX());
			float ry = (float)(cube.position.getY() - cameraPos.getY());
			float rz = (float)(cube.position.getZ() - cameraPos.getZ());
			
			// Offset edges to fix issues with z-fighting
			float offset = Math.max(MINIMUM_SURFACE_OFFSET, cube.dist * UNIT_SURFACE_OFFSET);

			// Draw transparent overlay
			int color = (cube.color & 0x00FFFFFF) | (COLOR_ALPHA << 24);
			renderer.fillCuboid(rx - offset,
			                    ry - offset,
			                    rz - offset,
			                    rx + 1.0f + offset,
			                    ry + 1.0f + offset,
			                    rz + 1.0f + offset,
			                    color);
			
			// Draw solid selection outline
			if (cube.selected) {
				renderSelectedOutline(renderer, rx, ry, rz, 2.0f * offset, 0xA0FFFFFF);
			}
		}
		
		renderer.finish();
	}
	
	private void renderSelectedOutline(GSIRenderer3D renderer, float rx, float ry, float rz, float offset, int color) {
		float rx0 = rx - offset;
		float ry0 = ry - offset;
		float rz0 = rz - offset;
		
		float rx1 = rx + 1.0f + offset;
		float ry1 = ry + 1.0f + offset;
		float rz1 = rz + 1.0f + offset;
		
		float r = GSColorUtil.unpackR(color) / 255.0f;
		float g = GSColorUtil.unpackG(color) / 255.0f;
		float b = GSColorUtil.unpackB(color) / 255.0f;
		float a = GSColorUtil.unpackA(color) / 255.0f;
		
		for (int i = 0; i < SELECTION_VERTICES.length; ) {
			float x0 = rx0 + (rx1 - rx0) * SELECTION_VERTICES[i++];
			float y0 = ry0 + (ry1 - ry0) * SELECTION_VERTICES[i++];
			float z0 = rz0 + (rz1 - rz0) * SELECTION_VERTICES[i++];
			
			renderer.vert(x0, y0, z0).color(r, g, b, a).next();
		}
	}
	
	@Override
	public GSERenderPhase getRenderPhase() {
		return GSERenderPhase.TRANSPARENT_LAST;
	}
	
	private static float[] computeSelectionVertices(float thickness, float notch) {
		// A single face of the selection in clockwise order for front
		// face and counter-clockwise for back face.
		float[] faceVertices = new float[] {
			// Bottom right
			0.0f, 0.0f,
			0.0f, thickness,
			notch, thickness,
			notch, 0.0f,

			0.0f, thickness,
			0.0f, notch,
			thickness, notch,
			thickness, thickness,

			// Top right
			0.0f, 1.0f,
			notch, 1.0f,
			notch, 1.0f - thickness,
			0.0f, 1.0f - thickness,

			0.0f, 1.0f - thickness,
			thickness, 1.0f - thickness,
			thickness, 1.0f - notch,
			0.0f, 1.0f - notch,
			
			// Bottom left
			1.0f, 0.0f,
			1.0f - notch, 0.0f,
			1.0f - notch, thickness,
			1.0f, thickness,
			
			1.0f, thickness,
			1.0f - thickness, thickness,
			1.0f - thickness, notch,
			1.0f, notch,
			
			// Top left
			1.0f, 1.0f,
			1.0f, 1.0f - thickness,
			1.0f - notch, 1.0f - thickness,
			1.0f - notch, 1.0f,
			
			1.0f, 1.0f - thickness,
			1.0f, 1.0f - notch,
			1.0f - thickness, 1.0f - notch,
			1.0f - thickness, 1.0f - thickness
		};
		
		// One x- and y-coordinate per vertex
		int vertexCount = faceVertices.length / 2;
		float[] vertices = new float[6 * 3 * vertexCount];
	
		int v = 0;
		
		// Back face
		for (int i = 0; i < vertexCount; i++) {
			vertices[v++] = faceVertices[2 * i + 0];
			vertices[v++] = faceVertices[2 * i + 1];
			vertices[v++] = 0.0f;
		}
		// Front face
		for (int i = 0; i < vertexCount; i++) {
			vertices[v++] = faceVertices[2 * i + 1];
			vertices[v++] = faceVertices[2 * i + 0];
			vertices[v++] = 1.0f;
		}
		// Left face
		for (int i = 0; i < vertexCount; i++) {
			vertices[v++] = 0.0f;
			vertices[v++] = faceVertices[2 * i + 0];
			vertices[v++] = faceVertices[2 * i + 1];
		}
		// Right face
		for (int i = 0; i < vertexCount; i++) {
			vertices[v++] = 1.0f;
			vertices[v++] = faceVertices[2 * i + 1];
			vertices[v++] = faceVertices[2 * i + 0];
		}
		// Bottom face
		for (int i = 0; i < vertexCount; i++) {
			vertices[v++] = faceVertices[2 * i + 1];
			vertices[v++] = 0.0f;
			vertices[v++] = faceVertices[2 * i + 0];
		}
		// Top face
		for (int i = 0; i < vertexCount; i++) {
			vertices[v++] = faceVertices[2 * i + 0];
			vertices[v++] = 1.0f;
			vertices[v++] = faceVertices[2 * i + 1];
		}
		
		return vertices;
	}
	
	private class GSCubeEntry {
		
		private final BlockPos position;
		private final int color;
		private final float dist;
		
		private boolean selected;
		
		public GSCubeEntry(BlockPos position, int color, float distSqr, boolean selected) {
			this.position = position;
			this.color = color;
			this.dist = distSqr;
			
			this.selected = selected;
		}
	}
}
