package com.npjt25.pissmod.gametest;

import java.util.List;

import net.minecraft.client.CameraType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.npjt25.pissmod.PeePhase;
import com.npjt25.pissmod.PissConfig;
import com.npjt25.pissmod.client.ClientStreams;
import com.npjt25.pissmod.client.PissModClient;
import com.npjt25.pissmod.server.PeeManager;
import com.npjt25.pissmod.server.StreamHits;

@SuppressWarnings("UnstableApiUsage")
public class PissModClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getConnection().waitForChunksRender();
			server.runCommand("time set noon");
			setCamera(context, CameraType.THIRD_PERSON_BACK);
			context.getInput().lookAt(0.0F, 10.0F);
			context.waitTicks(10);

			// Normal stream: key down -> unzip -> pee, key up -> finish -> zip -> idle.
			setPower(context, 0);
			context.getInput().holdKey(PissModClient.peeKey());
			context.waitTicks(4);
			assertPhase(server, PeePhase.UNZIPPING);
			context.waitTicks(20);
			assertPhase(server, PeePhase.PEEING);
			context.waitTicks(25);
			int streams = context.computeOnClient(client -> ClientStreams.activeCount());
			check(streams == 1, "client should be showing 1 stream, was " + streams);
			screenshotStream(context, CameraType.FIRST_PERSON, 40.0F, "pissmod-first-person-power-0");
			screenshotStream(context, CameraType.THIRD_PERSON_FRONT, 10.0F, "pissmod-front-power-0");
			setCamera(context, CameraType.THIRD_PERSON_BACK);
			context.getInput().lookAt(0.0F, 10.0F);
			context.getInput().releaseKey(PissModClient.peeKey());
			context.waitTicks(4);
			assertPhase(server, PeePhase.FINISHING);
			context.waitTicks(50);
			assertPhase(server, PeePhase.IDLE);

			// Below the damage threshold the stream hits but doesn't hurt; at 30 it does.
			int hitsBefore = server.computeOnServer(s -> StreamHits.entityHits);
			float healthAt25 = peeOnPig(context, server, 25);
			int hitsAt25 = server.computeOnServer(s -> StreamHits.entityHits) - hitsBefore;
			check(hitsAt25 > 0, "power 25 stream should hit the pig");
			check(healthAt25 >= 10.0F, "power 25 should not hurt, pig health was " + healthAt25);
			float healthAt30 = peeOnPig(context, server, 30);
			check(healthAt30 < 10.0F, "power 30 should hurt, pig health was " + healthAt30);
			peeOnPig(context, server, 50);

			// Firehose, seen from the front.
			server.runCommand("kill @e[type=minecraft:pig]");
			setPower(context, 50);
			context.getInput().holdKey(PissModClient.peeKey());
			context.waitTicks(20);
			screenshotStream(context, CameraType.THIRD_PERSON_FRONT, 0.0F, "pissmod-front-power-50");
			screenshotStream(context, CameraType.FIRST_PERSON, 0.0F, "pissmod-first-person-power-50");
			context.getInput().releaseKey(PissModClient.peeKey());
			context.waitTicks(45);
			assertPhase(server, PeePhase.IDLE);
		}
	}

	/** Puts a pig 3 blocks in front of the player (level with their feet), pees on it, and returns its health afterwards (0 if it died). */
	private static void setCamera(ClientGameTestContext context, CameraType camera) {
		context.runOnClient(client -> client.options.setCameraType(camera));
	}

	private static void screenshotStream(ClientGameTestContext context, CameraType camera, float pitch, String name) {
		setCamera(context, camera);
		context.getInput().lookAt(0.0F, pitch);
		context.waitTicks(25);
		context.takeScreenshot(name);
	}

	private static float peeOnPig(ClientGameTestContext context, TestServerContext server, int power) {
		server.runCommand("kill @e[type=minecraft:pig]");
		server.runCommand("execute as @p at @s rotated ~ 0 run summon minecraft:pig ^ ^ ^3 {NoAI:1b}");
		setPower(context, power);
		context.waitTicks(5);
		context.getInput().holdKey(PissModClient.peeKey());
		context.waitTicks(60);
		context.getInput().releaseKey(PissModClient.peeKey());
		context.waitTicks(45);
		return server.computeOnServer(PissModClientGameTest::pigHealth);
	}

	private static float pigHealth(MinecraftServer server) {
		List<? extends Pig> pigs = server.overworld().getEntities(EntityTypes.PIG, pig -> pig.isAlive());
		return pigs.isEmpty() ? 0.0F : pigs.get(0).getHealth();
	}

	private static void setPower(ClientGameTestContext context, int power) {
		context.runOnClient(client -> PissConfig.get().streamPower = power);
	}

	private static void assertPhase(TestServerContext server, PeePhase expected) {
		PeePhase actual = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().get(0);
			return PeeManager.phaseOf(player);
		});
		check(actual == expected, "expected phase " + expected + " but was " + actual);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
