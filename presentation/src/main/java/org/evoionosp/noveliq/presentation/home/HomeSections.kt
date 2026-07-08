package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import org.evoionosp.noveliq.domain.audiobook.model.Audiobook
import org.evoionosp.noveliq.presentation.R

@Composable
internal fun SectionBlock(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

@Composable
internal fun HorizontalBookRow(
    audiobooks: List<Audiobook>,
    accessToken: String,
    onOpenAudiobook: (Audiobook) -> Unit
) {
    if (audiobooks.isEmpty()) {
        PlaceholderSectionCard(
            title = stringResource(R.string.home_section_empty_title),
            body = stringResource(R.string.home_section_empty_body)
        )
        return
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(
            items = audiobooks,
            key = { it.id }
        ) { audiobook ->
            AudiobookCarouselCard(
                audiobook = audiobook,
                accessToken = accessToken,
                onClick = { onOpenAudiobook(audiobook) }
            )
        }
    }
}

@Composable
internal fun PlaceholderSectionCard(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun StatsRow(
    booksCount: Int,
    authorsCount: Int,
    hoursCount: Double
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                title = stringResource(R.string.stats_books),
                value = booksCount.toString(),
                modifier = Modifier.fillParentMaxWidth(0.35f)
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_authors),
                value = authorsCount.toString(),
                modifier = Modifier.fillParentMaxWidth(0.35f)
            )
        }
        item {
            StatCard(
                title = stringResource(R.string.stats_hours),
                value = String.format("%.1f", hoursCount),
                modifier = Modifier.fillParentMaxWidth(0.35f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AudiobookCarouselCard(
    audiobook: Audiobook,
    accessToken: String,
    onClick: () -> Unit
) {
    AudiobookGridCard(
        audiobook = audiobook,
        accessToken = accessToken,
        onClick = onClick,
        coverAspectRatio = 0.72f,
        modifier = Modifier
            .width(150.dp)
    )
}
