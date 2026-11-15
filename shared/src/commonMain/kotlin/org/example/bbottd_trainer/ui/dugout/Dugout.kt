package org.example.bbottd_trainer.ui.dugout

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.presentation.DugoutSection
import org.example.bbottd_trainer.presentation.label
import org.example.bbottd_trainer.ui.SelectionColor
import org.example.bbottd_trainer.ui.player.PlayerTokenView
import org.example.bbottd_trainer.ui.player.displayColor

private val DugoutTokenSize = 30.dp
private val SectionShape = RoundedCornerShape(6.dp)

/**
 * One team's dugout: a section per dugout status, each listing that team's players.
 *
 * Each section is a click target (moving the selected player into it), highlighted while
 * [isTarget]; each token can be clicked to select that player.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Dugout(
    teamName: String,
    teamColor: TeamColor,
    sections: List<DugoutSection>,
    modifier: Modifier = Modifier,
    selectedPlayerId: PlayerId? = null,
    isTarget: Boolean = false,
    onSectionClick: (PlayerStatus) -> Unit = {},
    onPlayerClick: (PlayerId) -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$teamName dugout",
                style = MaterialTheme.typography.titleSmall,
                color = teamColor.displayColor,
            )
            for (section in sections) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SectionShape)
                        .border(
                            width = if (isTarget) 2.dp else 1.dp,
                            color = if (isTarget) SelectionColor else MaterialTheme.colorScheme.outlineVariant,
                            shape = SectionShape,
                        )
                        .clickable { onSectionClick(section.status) }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${section.status.label} (${section.players.size})",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    FlowRow(
                        // At least one token row high, so an empty section is as tall as one
                        // holding a player and stays an easy target.
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = DugoutTokenSize),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (token in section.players) {
                            key(token.playerId) {
                                PlayerTokenView(
                                    token = token,
                                    size = DugoutTokenSize,
                                    selected = token.playerId == selectedPlayerId,
                                    modifier = Modifier.clickable { onPlayerClick(token.playerId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
