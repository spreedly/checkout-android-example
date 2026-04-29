package com.spreedly.example.screens.mainmenu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.spreedly.app.BuildConfig
import com.spreedly.app.R
import com.spreedly.example.JavaHostedFieldsActivity
import com.spreedly.example.JavaOffsitePaymentActivity
import com.spreedly.example.TraditionalActivity
import com.spreedly.example.ui.components.BulletPoint
import com.spreedly.example.ui.components.MenuItemCard
import com.spreedly.example.ui.theme.Spacing

@SuppressLint("ComposeModifierMissing")
@Composable
fun MainMenuScreen(navController: NavHostController) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.screenPadding)
                .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Header Section
        Text(
            text = stringResource(R.string.main_menu_title),
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            modifier = Modifier
                .padding(vertical = Spacing.lg)
                .semantics {
                    heading()
                },
        )

        Text(
            text = stringResource(R.string.main_menu_section_header),
            style =
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                ),
            modifier = Modifier.padding(bottom = Spacing.xl),
        )

        MenuItemCard(
            title = stringResource(R.string.menu_item_payment_bottom_sheet_title),
            description = stringResource(R.string.menu_item_payment_bottom_sheet_description),
            onClick = { navController.navigate("home_screen") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_reusable_bottom_sheet_title),
            description = stringResource(R.string.menu_item_reusable_bottom_sheet_description),
            onClick = { navController.navigate("reusable_bottom_sheet") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_basic_form_title),
            description = stringResource(R.string.menu_item_basic_form_description),
            onClick = { navController.navigate("basic_checkout") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_bank_account_title),
            description = stringResource(R.string.menu_item_bank_account_description),
            onClick = { navController.navigate("bank_account") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_headless_bank_account_title),
            description = stringResource(R.string.menu_item_headless_bank_account_description),
            onClick = { navController.navigate("headless_bank_account_flow") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_additional_fields_title),
            description = stringResource(R.string.menu_item_additional_fields_description),
            onClick = { navController.navigate("custom_checkout") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_custom_theme_title),
            description = stringResource(R.string.menu_item_custom_theme_description),
            onClick = { navController.navigate("customized_checkout") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_flexible_expiry_title),
            description = stringResource(R.string.menu_item_flexible_expiry_description),
            onClick = { navController.navigate("flexible_expiry") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_custom_text_fields_title),
            description = stringResource(R.string.menu_item_custom_text_fields_description),
            onClick = { navController.navigate("custom_text_fields") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_recaching_showcase_title),
            description = stringResource(R.string.menu_item_recaching_showcase_description),
            onClick = { navController.navigate("recaching_showcase") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_threeds_custom_tabs_challenge_title),
            description = stringResource(R.string.menu_item_threeds_custom_tabs_challenge_description),
            onClick = { navController.navigate("threeds_challenge") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_threeds_challenge_title),
            description = stringResource(R.string.menu_item_threeds_challenge_description),
            onClick = {
                navController.navigate("threeds_global_challenge")
            },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_offsite_payment_title),
            description = stringResource(R.string.menu_item_offsite_payment_description),
            onClick = { navController.navigate("offsite_payment") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_ebanx_payment_title),
            description = stringResource(R.string.menu_item_ebanx_payment_description),
            onClick = { navController.navigate("ebanx_payment") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_stripe_apm_payment_title),
            description = stringResource(R.string.menu_item_stripe_apm_payment_description),
            onClick = { navController.navigate("stripe_apm_payment") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_braintree_payment_title),
            description = stringResource(R.string.menu_item_braintree_payment_description),
            onClick = { navController.navigate("braintree_payment") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_design_system_title),
            description = stringResource(R.string.menu_item_design_system_description),
            onClick = { navController.navigate("design_system") },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_traditional_xml_title),
            description = stringResource(R.string.menu_item_traditional_xml_description),
            onClick = {
                val intent = Intent(context, TraditionalActivity::class.java)
                context.startActivity(intent)
            },
        )

        Spacer(Modifier.height(Spacing.md))

        MenuItemCard(
            title = stringResource(R.string.menu_item_java_hosted_fields_title),
            description = stringResource(R.string.menu_item_java_hosted_fields_description),
            onClick = {
                val intent = Intent(context, JavaHostedFieldsActivity::class.java)
                context.startActivity(intent)
            },
        )

        Spacer(Modifier.height(Spacing.md))

        if (BuildConfig.ENABLE_JAVA_OFFSITE_PAYMENT) {
            MenuItemCard(
                title = stringResource(R.string.menu_item_java_offsite_title),
                description = stringResource(R.string.menu_item_java_offsite_description),
                onClick = {
                    val intent = Intent(context, JavaOffsitePaymentActivity::class.java)
                    context.startActivity(intent)
                },
            )
            Spacer(Modifier.height(Spacing.md))
        }

        Spacer(modifier = Modifier.height(Spacing.mlg))

        // About Section
        Text(
            text = stringResource(R.string.about_section_header),
            style =
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                ),
            modifier = Modifier.padding(bottom = Spacing.md),
        )

        Text(
            text = stringResource(R.string.about_title),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            modifier = Modifier.padding(bottom = Spacing.xs),
        )

        Text(
            text = stringResource(R.string.about_description),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            BulletPoint(stringResource(R.string.feature_complete_checkout))
            BulletPoint(stringResource(R.string.feature_headless_ui))
            BulletPoint(stringResource(R.string.feature_custom_forms))
            BulletPoint(stringResource(R.string.feature_secure_fields))
            BulletPoint(stringResource(R.string.feature_traditional_xml))
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}
