package org.evoionosp.noveliq.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
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

    HorizontalUncontainedCarousel(
        state = rememberCarouselState { audiobooks.size },
        modifier = Modifier.fillMaxWidth(),
        itemWidth = 150.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { index ->
        AudiobookCarouselCard(
            audiobook = audiobooks[index],
            accessToken = accessToken,
            onClick = { onOpenAudiobook(audiobooks[index]) }
        )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(304.dp)
    ) {
        AudiobookGridCard(
            audiobook = audiobook,
            accessToken = accessToken,
            onClick = onClick,
            coverAspectRatio = 0.72f
        )
    }
}
