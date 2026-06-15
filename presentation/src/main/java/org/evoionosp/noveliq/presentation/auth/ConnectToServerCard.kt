package org.evoionosp.noveliq.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.evoionosp.noveliq.presentation.R
import java.lang.reflect.Modifier
import java.util.Locale.getDefault

@Composable
fun ConnectToServerCard(
    state: AuthUiState,
    onProtocolChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    ) {

    val https = stringResource(R.string.https_protocol)
    val http = stringResource(R.string.http_protocol)

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                text = stringResource(R.string.connect_to_server),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            SingleChoiceSegmentedButtonRow(
                content = {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        onClick = {onProtocolChange(https)},
                        selected = state.protocol.equals(https, true),
                        label = { Text(text = https.uppercase(getDefault())) }
                    )
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        onClick = {onProtocolChange(http)},
                        selected = state.protocol.equals(http, true),
                        label = { Text(text = http.uppercase(getDefault())) }
                    )
                }
            )
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onUrlChange,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                prefix = { Text(text = state.protocol) },
                label = { Text(stringResource(R.string.server_url_label)) },
                placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Button(
                onClick = onSubmit,
                enabled = !state.isChecking,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isChecking) {
                        CircularProgressIndicator(
                            modifier = androidx.compose.ui.Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                        Text(stringResource(R.string.checking_server))
                    } else {
                        Text(stringResource(R.string.check_server))
                    }
                }
            }
        }
    }
}