package com.akon.lives;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class LivesAPI {

	private final String OBJECTIVE_NAME = "lives_objective";
	@Getter(value = AccessLevel.PRIVATE, lazy = true)
	private final Objective livesObjective = initObjective();

	public int getLives(Player player) {
		Score score = getLivesObjective().getScore(player.getName());
		if (!score.isScoreSet()) {
			score.setScore(LivesMain.getInstance().getConfig().getInt("default-lives", 5));
		}
		return score.getScore();
	}

	public void setLives(Player player, int lives) {
		getLivesObjective().getScore(player.getName()).setScore(Math.max(0, lives));
	}

	private Objective initObjective() {
		Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
		return Optional.ofNullable(scoreboard.getObjective(OBJECTIVE_NAME)).orElseGet(() -> {
			Objective objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy", "残機");
			objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
			return objective;
		});
	}

}
