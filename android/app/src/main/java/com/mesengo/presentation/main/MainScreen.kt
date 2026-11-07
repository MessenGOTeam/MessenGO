package com.mesengo.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.mesengo.presentation.chat.ConversationListScreen
import com.mesengo.presentation.contacts.ContactsScreen
import com.mesengo.presentation.main.components.BottomNavBar
import com.mesengo.presentation.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToChat: (conversationId: String, peerUserId: String, peerDisplayName: String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val totalUnread by viewModel.totalUnreadCount.collectAsState()
    val pendingRequests by viewModel.pendingRequestsCount.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    val topBarTitle = when (selectedTab) {
        MainTab.CHATS -> "MessenGO"
        MainTab.CONTACTS -> "Danh bạ"
        MainTab.PROFILE -> "Hồ sơ cá nhân"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) },
                unreadChatsCount = totalUnread,
                pendingRequestsCount = pendingRequests
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.CHATS -> {
                    ConversationListScreen(
                        conversations = conversations,
                        onConversationClick = { convId, peerId, peerName ->
                            onNavigateToChat(convId, peerId, peerName)
                        }
                    )
                }
                MainTab.CONTACTS -> {
                    ContactsScreen(
                        contacts = contacts,
                        onContactClick = { contact ->
                            val convId = "conv_${contact.contactUserId}"
                            onNavigateToChat(convId, contact.contactUserId, contact.displayName)
                        },
                        onAddFriendClick = {
                            // Dialog thêm bạn bè sẽ tích hợp ở Bước 2
                        }
                    )
                }
                MainTab.PROFILE -> {
                    ProfileScreen(
                        onLogout = {
                            viewModel.logout(onLogout)
                        }
                    )
                }
            }
        }
    }
}
