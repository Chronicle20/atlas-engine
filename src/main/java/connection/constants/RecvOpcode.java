/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package connection.constants;

public enum RecvOpcode {

    // CLogin::SendCheckPasswordPacket
    LOGIN_PASSWORD(0x01),
    GUEST_LOGIN(0x02),

    // CLogin::ChangeStepImmediate
    SERVERLIST_REREQUEST(0x03),

    // CLogin::SendLoginPacket
    CHARLIST_REQUEST(0x04),

    // 0x05 CLogin::SendViewAllCharPacket
    // CLogin::GotoWorldSelect
    SERVERSTATUS_REQUEST(0x05),

    // CLogin::SendSelectCharPacket
    CHAR_SELECT(0x06),

    // CClientSocket::OnConnect
    PLAYER_LOGGEDIN(0x07),

    // CCashShop::SendCheckNameChangePossiblePacket
    // CLogin::SendCheckDuplicateIDPacket
    CHECK_CHAR_NAME(0x08),
    NAME_TRANSFER(0x09),

    // CLogin::SendNewCharPacket
    CREATE_CHAR(0x0B),

    // 0x0C CLogin::SendNewCharPacket

    // CLogin::SendDeleteCharPacket
    DELETE_CHAR(0x0D),

    // 0x11 - sub_4B1BB3

    // 0x13 - CLogin::SendSelectCharPacket
    // sub_67219A
    // sub_67226D
    // 0x14 - CLogin::SendSelectCharPacket

    // CClientSocket::OnConnect
    CLIENT_START_ERROR(0x15),

    // CLogin::Init
    // CLogin::GotoTitle
    REACHED_LOGIN_SCREEN(0x18),

    // CClientSocket::OnAliveReq
    PONG(0x0E),
    ACCEPT_TOS(0xFFFF),
    SET_GENDER(0xFFFF),
    AFTER_LOGIN(0xFFFF),
    REGISTER_PIN(0xFFFF),
    SERVERLIST_REQUEST(0xFFFF),
    PLAYER_DC(0xFFFF),

    // CLogin::SendViewAllCharPacket
    VIEW_ALL_CHAR(0x0A),
    PICK_ALL_CHAR(0xFFFF),

    WORLD_TRANSFER(0xFFFF),

    // 0x19 CLogin::OnCheckPasswordResult
    // sub_671717

    // CLogin::CLogin
    CREATE_SECURITY_HANDLE(0x1A),
    CLIENT_ERROR(0xFFFF),
    STRANGE_DATA(0xFFFF),
    RELOG(0xFFFF),
    REGISTER_PIC(0xFFFF),
    CHAR_SELECT_WITH_PIC(0xFFFF),
    VIEW_ALL_PIC_REGISTER(0xFFFF),
    VIEW_ALL_WITH_PIC(0xFFFF),
    PACKET_ERROR(0x25),

    // CCashShop::SendTransferFieldPacket
    // CField::SendTransferFieldRequest
    // CITC::SendTransferFieldPacket
    CHANGE_MAP(0x1D),

    // CField::SendTransferChannelRequest
    CHANGE_CHANNEL(0x1E),

    // CWvsContext::SendMigrateToShopRequest
    ENTER_CASHSHOP(0x1F),
    MOVE_PLAYER(0x20),

    // CUserLocal::HandleXKeyDown
    // CWvsContext::SendGetUpFromChairRequest
    CANCEL_CHAIR(0x21),

    // CWvsContext::SendSitOnPortableChairRequest
    USE_CHAIR(0x22),
    CLOSE_RANGE_ATTACK(0x23),
    RANGED_ATTACK(0x24),
    MAGIC_ATTACK(0x25),
    TOUCH_MONSTER_ATTACK(0x26),

    // CUserLocal::Update
    // CUserLocal::SetDamaged
    TAKE_DAMAGE(0x27),

    // CField::SendChatMsg
    GENERAL_CHAT(0x29),

    // CUserLocal::HandleLButtonClk
    CLOSE_CHALKBOARD(0x2A),

    // CWvsContext::SendEmotionChange
    FACE_EXPRESSION(0x2B),

    // CWvsContext::SendActiveEffectItemChange
    USE_ITEMEFFECT(0x2C),

    // CUserLocal::RequestUpgradeTombEffect
    USE_DEATHITEM(0x2D),

    // CUserLocal::SendBanMapByMobRequest
    MOB_BANISH_PLAYER(0x30),
    MONSTER_BOOK_COVER(0x31),

    // 0x31 sub_8FA27F

    // CNpc::ShowQuestList
    // CUserLocal::TalkToNpc
    NPC_TALK(0x32),

    // CWvsContext::SendRemoteShopOpenRequest
    REMOTE_STORE(0x33),

    // CScriptMan::OnSay
    // CScriptMan::OnSayImage
    // CScriptMan::OnAskYesNo
    // CScriptMan::OnAskText
    // CScriptMan::OnAskBoxText
    // CScriptMan::OnAskNumber
    // CScriptMan::OnAskMenu
    // CScriptMan::OnAskAvatar
    // CScriptMan::OnAskPet
    // CScriptMan::OnAskPetAll
    // CScriptMan::OnAskSlideMenu
    // CUIInitialQuiz::SendResult
    // CUISpeedQuiz::SendResult
    NPC_TALK_MORE(0x34),

    // CShopDlg::SetRet
    // CShopDlg::SendBuyRequest
    // CShopDlg::SendSellRequest
    // sub_7CAB93
    // CShopDlg::SendRechargeRequest
    NPC_SHOP(0x35),

    // CTrunkDlg::SetRet
    // CTrunkDlg::SendGetItemRequest
    // CTrunkDlg::SendPutItemRequest
    // CTrunkDlg::SendSortItemRequest
    // CTrunkDlg::SendGetMoneyRequest
    // CTrunkDlg::SendPutMoneyRequest
    STORAGE(0x36),
    HIRED_MERCHANT_REQUEST(0x37),

    // CStoreBankDlg::SetRet
    // CStoreBankDlg::SendCalculateFeeRequest
    // CStoreBankDlg::SendGetAllRequest
    // CStoreBankDlg::OnPacket
    FREDRICK_ACTION(0x38),

    // CUIFadeYesNo::OnButtonClicked
    DUEY_ACTION(0x39),

    // CUIShopScanner::OnCreate
    OWL_ACTION(0x3A),
    // CUIShopScanResult::OnButtonClicked
    OWL_WARP(0x3B),
    // 0x3C CUIAccountMoreInfo::SendLoadAccountMoreInfoRequest

    // CAdminShopDlg::OnPacket
    // CAdminShopDlg::SetRet
    // CAdminShopDlg::SendTradeRequest
    ADMIN_SHOP(0x3C),

    // CWvsContext::SendGatherItemRequest
    ITEM_SORT(0x3D),

    // CWvsContext::SendSortItemRequest
    ITEM_SORT2(0x3E),

    // CWvsContext::SendChangeSlotPositionRequest
    ITEM_MOVE(0x3F),

    // CWvsContext::SendStatChangeItemUseRequest
    USE_ITEM(0x40),

    // CWvsContext::SendStatChangeItemCancelRequest
    CANCEL_ITEM_EFFECT(0x41),

    //0x42 - CWvsContext::TryRecovery

    // CWvsContext::SendMobSummonItemUseRequest
    USE_SUMMON_BAG(0x43),

    // CWvsContext::SendPetFoodItemUseRequest
    PET_FOOD(0x44),

    // CWvsContext::SendTamingMobFoodItemUseRequest
    USE_MOUNT_FOOD(0x45),
    SCRIPTED_ITEM(0x46),

    // CItemSpeakerDlg::_SendConsumeCashItemUseRequest
    // CUIFindFriendDetail::SetDetailInfo
    // CUIKarmaDlg::_SendConsumeCashItemUseRequest
    // CUIQuestInfoDetail::OnButtonClicked
    USE_CASH_ITEM(0x47),

    // 0x48 - CWvsContext::SendActivatePetRequest

    // CWvsContext::SendBridleItemUseRequest
    USE_CATCH_ITEM(0x49),

    // CWvsContext::SendSkillLearnItemUseRequest
    USE_SKILL_BOOK(0x4A),

    // CWvsContext::SendShopScannerItemUseRequest
    // 0x4B

    // CWvsContext::SendMapTransferItemUseRequest
    USE_TELEPORT_ROCK(0x4C),

    // CWvsContext::SendPortalScrollUseRequest
    USE_RETURN_SCROLL(0x4D),

    // 0x4E sub_AEDDCB
    // 0x4F sub_AEDD3A
    // 0x50 CWvsContext::SendItemReleaseRequest

    // 0x51 sub_AEDCA9
    USE_UPGRADE_SCROLL(0x4E),

    // CWvsContext::SendAbilityUpRequest
    DISTRIBUTE_AP(0x52),

    // CWvsContext::SendAbilityUpRequest
    AUTO_DISTRIBUTE_AP(0x53),

    // CWvsContext::SendStatChangeRequestByItemOption
    HEAL_OVER_TIME(0x54),

    // CWvsContext::SendSkillUpRequest
    DISTRIBUTE_SP(0x55),

    // CUserLocal::DoActiveSkill_TownPortal
    // CUserLocal::DoActiveSkill_StatChangeAdmin
    // CUserLocal::DoActiveSkill_Heal
    // CUserLocal::DoActiveSkill_Summon
    // CUserLocal::TryDoingMonsterMagnet
    // CUserLocal::DoActiveSkill_SmokeShell
    // CUserLocal::DoActiveSkill_RecoveryAura
    // CUserLocal::DoActiveSkill_Flying
    // CUserLocal::DoActiveSkill_DamageMeter
    // CUserLocal::SendSkillUseRequest
    // sub_A3ED44
    // CGrenade::SendTimeBombInfo
    SPECIAL_MOVE(0x56),

    // CUserLocal::SendSkillCancelRequest
    CANCEL_BUFF(0x57),

    // CUserLocal::DoActiveSkill_Prepare
    SKILL_EFFECT(0x58),

    // CWvsContext::SendDropMoneyRequest
    MESO_DROP(0x59),

    // CWvsContext::SendGivePopularityRequest
    GIVE_FAME(0x5A),

    // CWvsContext::SendCharacterInfoRequest
    CHAR_INFO_REQUEST(0x5C),

    // CWvsContext::SendActivatePetRequest
    SPAWN_PET(0x5D),

    // CWvsContext::CheckTemporaryStatDuration
    CANCEL_DEBUFF(0x5E),

    // CUserLocal::CheckPortal_Collision
    // CUserLocal::HandleUpKeyDown
    CHANGE_MAP_SPECIAL(0x5F),

    // CUserLocal::TryRegisterTeleport
    USE_INNER_PORTAL(0x60),

    // CWvsContext::SendMapTransferRequest
    TROCK_ADD_MAP(0x61),

    // 0x62 - CWvsContext::SendAntiMacroItemUseRequest
    REPORT(0x63),

    // 0x64 CUIAntiMacro::SetRet
    // CUIAdminAntiMacro::SetRet

    // CQuest::StartQuest
    // CQuest::OnCompleteQuestFailed
    // CWvsContext::ResignQuest
    QUEST_ACTION(0x66),


    // 0x67 - CWvsContext::OnTemporaryStatSet
    // CWvsContext::OnTemporaryStatReset
    // CWvsContext::CheckDarkForce
    // CWvsContext::CheckDragonFury

    // CUserLocal::ThrowGrenade
    GRENADE_EFFECT(0x68),

    // CMacroSysMan::FlushToSvr
    SKILL_MACRO(0x69),

    // 0x6A - CWvsContext::SendSelectNpcItemUseRequest
    // 0x62 - CWvsContext::SendAntiMacroItemUseRequest
    // 0x63 - CUserLocal::DoAntiMacroSkill

    // CWvsContext::SendLotteryItemUseRequest
    USE_ITEM_REWARD(0x6B),

    // CUIItemMaker::RequestItemMake
    MAKER_SKILL(0x6C),

    // 0x6D - CRepairDurabilityDlg::SendRepairDurabilityAll
    // 0x6E - CRepairDurabilityDlg::SendRepairDurability
    // 0x70 - CUserLocal::UpdateClientTimer
    // 0x71 - CWvsContext::SendExpUpItemUseRequest
    // 0x72 - CWvsContext::SendTempExpUseRequest
    // 0x73 - sub_AF8B98
    // 0x74 - CWvsContext::SendFollowCharacterRequest
    // 0x75 - sub_B0BBA8
    // 0x76 - CWvsContext::SendUseBoxGachaponItemRequest
    // CWvsContext::OnSetPassenserRequest
    USE_REMOTE(0x77),
    WATER_OF_LIFE(0x78),

    // CField::SendChatMsgSlash
    ADMIN_CHAT(0x78),

    // CUIStatusBar::SendGroupMessage
    MULTI_CHAT(0x79),

    // CField::SendChatMsgWhisper
    // CField::SendLocationWhisper
    // CField::OnWhisper
    WHISPER(0x7A),

    // 0x7B CDamageMeter::SaveDamageInfo
    SPOUSE_CHAT(0x7F),

    // CFadeWnd::SendCloseMessage
    // CUIMessenger::OnCreate
    // CUIMessenger::OnDestroy
    // CUIMessenger::Update
    // CUIMessenger::OnInvite
    // CUIMessenger::SendInviteMsg
    // CUIMessenger::ProcessChat
    MESSENGER(0x7B),

    // CCashTradingRoomDlg::SetRet
    // CCashTradingRoomDlg::OnTrade
    // CCashTradingRoomDlg::PutItem
    // CCashTradingRoomDlg::PutMoney
    // CCashTradingRoomDlg::Trade
    // CEntrustedShopDlg::SetRet
    // CEntrustedShopDlg::OnCorrectSSN2
    // CEntrustedShopDlg::OnGoOut
    // CEntrustedShopDlg::OnArrange
    // CEntrustedShopDlg::OnWithdrawMoney
    // CEntrustedShopDlg::OnBlackList
    // CEntrustedShopDlg::OnVisitList
    // CEntrustedShopDlg::AddBlackList
    // CEntrustedShopDlg::DeleteBlackList
    // CField::SendInviteTradingRoomMsg
    // CField::AddBlackList
    // CField::DeleteBlackList
    // CMemoryGameDlg::OnEnterResult
    // CMemoryGameDlg::OnTieRequest
    // CMemoryGameDlg::SendTurnUpCard
    // CMemoryGameDlg::Update
    // CMemoryGameDlg::SendClaimGiveUp
    // CMemoryGameDlg::SendTieRequest
    // CMemoryGameDlg::OnClickStartButton
    // CMemoryGameDlg::OnClickReadyButton
    // CMemoryGameDlg::OnClickBanButton
    // CMemoryGameDlg::OnClickEndButton
    // CMiniRoomBaseDlg::SendInviteResult
    // CMiniRoomBaseDlg::SendCashInviteResult
    // CMiniRoomBaseDlg::CheckAndSendChat
    // COmokDlg::OnEnterResult
    // COmokDlg::OnTieRequest
    // COmokDlg::OnRetreatRequest
    // COmokDlg::Update
    // COmokDlg::PutStoneChecker
    // COmokDlg::SendClaimGiveUp
    // COmokDlg::SendTieRequest
    // COmokDlg::SendRetreatRequest
    // COmokDlg::OnClickStartButton
    // COmokDlg::OnClickReadyButton
    // COmokDlg::OnClickBanButton
    // COmokDlg::OnClickEndButton
    // CPersonalShopDlg::SetRet
    // CPersonalShopDlg::OnCorrectSSN2
    // CPersonalShopDlg::BuyItem
    // CPersonalShopDlg::PutItem
    // CPersonalShopDlg::MoveItemToInventory
    // CPersonalShopDlg::DeliverBlackList
    // CPersonalShopDlg::OnClickBanButton
    // CPersonalShopDlg::Update
    // CTradingRoomDlg::SetRet
    // CTradingRoomDlg::OnTrade
    // CTradingRoomDlg::PutItem
    // CTradingRoomDlg::PutMoney
    // CTradingRoomDlg::Trade
    // CUserLocal::HandleLButtonDblClk\
    // CUserLocal::HandleRButtonClk
    // CWvsContext::SendCreateMiniGameRequest
    // CWvsContext::SendOpenShopRequest
    // CWvsContext::OnEntrustedShopCheckResult
    PLAYER_INTERACTION(0x7C),

    // CField::SendCreateNewPartyMsg
    // CField::SendWithdrawPartyMsg
    // CField::SendJoinPartyMsg
    // CField::SendKickPartyMsg
    // CField::SendChangePartyBossMsg
    // CWvsContext::OnPartyResult
    PARTY_OPERATION(0x7D),

    // CFadeWnd::SendCloseMessage
    // CUIFadeYesNo::OnButtonClicked
    // CWvsContext::OnPartyResult
    DENY_PARTY_REQUEST(0x7E),

    // 0x7F - ExpeditionIntermediary::SendResponseInvitePacket
    // ExpeditionIntermediary::SendExpChangeMasterPacket
    // ExpeditionIntermediary::SendExpChangeBossPacket
    // ExpeditionIntermediary::SendExpRelocatePartyPacket
    // ExpeditionIntermediary::SendExpKickPacket
    // ExpeditionIntermediary::SendExpInvitePacket
    // ExpeditionIntermediary::SendExpCreatePacket
    // ExpeditionIntermediary::SendWithdrawPacket
    // ExpeditionIntermediary::OnPacketExpNoti_Invite


    // 0x80 - CUIFadeYesNo::OnButtonClicked
    // CUIFadeYesNo::SendCloseMessage
    // TabPartyAdver::SendPartyAdverRequestPacket
    // TabPartyAdver::SendPartyRegistCommitPacket
    // TabPartyAdver::RequestAdverRemoveFromNotiList
    // TabPartyAdver::SendPartyApplyPacketTo
    // TabPartyAdver::SendAdverDeletePacket
    // CWvsContext::OnPartyResult

    // CUIFadeYesNo::OnButtonClicked
    // CField::InputGuildName
    // CField::SendCreateGuildAgreeMsg
    // CField::SendInviteGuildMsg
    // CField::SendWithdrawGuildMsg
    // CField::SendKickGuildMsg
    // sub_56E0B9
    // CField::SendSetMemberGradeMsg
    // CField::SendSetGradeNameMsg
    // CField::SendSetGuildMarkMsg
    // CField::SendSetGuildNoticeMsg
    // CWvsContext::OnGuildResult
    GUILD_OPERATION(0x81),

    // CFadeWnd::SendCloseMessage
    // CWvsContext::OnGuildResult
    DENY_GUILD_REQUEST(0x82),

    // CField::SendChatMsgSlash
    ADMIN_COMMAND(0x83),
    ADMIN_LOG(0x89),

    // CField::SendSetFriendMsg
    // CField::SendDeleteFriendMsg
    // CField::SendAcceptFriendMsg
    // CWvsContext::LoadFriend
    BUDDYLIST_MODIFY(0x85),

    // CWvsContext::OnMemoNotify_Receive
    NOTE_ACTION(0x86),

    // CField::TryEnterTownPortal
    // CTownPortalPool::TryEnterTownPortal
    USE_DOOR(0x88),

    // CFuncKeyMappedMan::SaveFuncKeyMap
    // CFuncKeyMappedMan::ChangePetConsumeItemID
    // CFuncKeyMappedMan::ChangePetConsumeMPItemID
    // sub_5E7D2D
    CHANGE_KEYMAP(0x8A),

    // CRPSGameDlg::OnBtStart
    // CRPSGameDlg::OnBtContinue
    // CRPSGameDlg::OnBtRetry
    // CRPSGameDlg::OnBtExit
    // CRPSGameDlg::SendSelection
    RPS_ACTION(0x8B),

    // CEngageDlg::SetRet
    // CWvsContext::SendInvitationQuery
    // CWvsContext::SendSendInvitaionRequest
    // CWvsContext::SendRingDropRequest
    // CWvsContext::OnMarriageRequest
    RING_ACTION(0x8C),

    // CWishListGiveDlg::SetRet
    // CWishListGiveDlg::SendPutItemRequest
    // CWishListRecvDlg::SetRet
    // CWishListRecvDlg::SendGetItemRequest
    WEDDING_ACTION(0x8D),

    // 0x8F - CWvsContext::SendBoobyTrapAlert

    // 0x90 - CUIMiniMap::OnMouseButton

    // CUIFadeYesNo::OnButtonClicked
    // CTabGuildAlliance::OnInvite
    // CTabGuildAlliance::OnWithdraw
    // CTabGuildAlliance::OnKick
    // CTabGuildAlliance::OnGradeChange
    // CTabGuildAlliance::OnSetNotice
    // CTabGuildAlliance::OnChangeMaster
    // CWndAllianceGrade::OnSaveGradeName
    // CWvsContext::OnGuildResult
    // CWvsContext::OnAllianceResult
    ALLIANCE_OPERATION(0x91),

    // 0x9F - CWvsContext::SendRequestSessionValue

    // CFadeWnd::SendCloseMessage
    // CWvsContext::OnAllianceResult
    DENY_ALLIANCE_REQUEST(0x92),

    // CWvsContext::SendFamilyChartRequest
    OPEN_FAMILY_PEDIGREE(0x93),

    // CWvsContext::SendFamilyInfoRequest
    OPEN_FAMILY(0x94),

    // CWvsContext::SendRegisterJunior
    ADD_FAMILY(0x95),

    // CWvsContext::SendUnregisterJunior
    SEPARATE_FAMILY_BY_SENIOR(0x96),

    // CWvsContext::SendUnregisterParent
    SEPARATE_FAMILY_BY_JUNIOR(0x97),

    // CWvsContext::SendFamilyInviteResult
    ACCEPT_FAMILY(0x98),

    // CWvsContext::SendUseFamilyPrivilege
    USE_FAMILY(0x99),

    // 0xA7 - CCashShop::SendCangeMaplePoint

    // CWvsContext::SendSetFamilyPrecept
    CHANGE_FAMILY_MESSAGE(0x9A),

    // CWvsContext::OnFamilySummonRequest
    FAMILY_SUMMON_RESPONSE(0x9B),

    // 0xA2 sub_5FD2DB
    // 0xA3 sub_AF8F90
    BBS_OPERATION(0xA3),

    // CWvsContext::SendMigrateToITCRequest
    ENTER_MTS(0xA6),
    USE_SOLOMON_ITEM(0xFFFF),
    USE_GACHA_EXP(0xFFFF),
    NEW_YEAR_CARD_REQUEST(0xA7),

    // CUICashItemGachapon::OnButtonClicked
    CASHSHOP_SURPRISE(0xA7),

    // 0xA8 sub_5EB1BA
    // 0xAA CUICashGachapon::OnButtonClicked

    // CUserLocal::HandleLButtonDblClk
    CLICK_GUIDE(0x9C),

    // CUserLocal::RequestIncCombo
    ARAN_COMBO_COUNTER(0x9D),

    // 0x9E - CMobPool::OnMobCrcKeyChanged
    // 0xBC CNpc::Init
    // 0xBD sub_AF9224

    // CVecCtrlPet::EndUpdateActive
    MOVE_PET(0xAA),

    // CPet::DoAction
    PET_CHAT(0xAB),

    // CPet::ParseCommand
    PET_COMMAND(0xAC),

    // CPet::SendDropPickUpRequest
    PET_LOOT(0xAD),

    // CWvsContext::SendStatChangeItemUseRequestByPetQ
    PET_AUTO_POT(0xAE),

    // CPet::SendUpdateExceptionListRequest
    PET_EXCLUDE_ITEMS(0xAF),

    // CVecCtrlSummoned::EndUpdateActive
    MOVE_SUMMON(0xB2),
    SUMMON_ATTACK(0xB3),

    // CSummoned::SetDamaged
    DAMAGE_SUMMON(0xB4),

    // CSummoned::TryDoingHeal
    // CSummoned::TryDoingGiveBuff
    // CSummoned::TryDoingHealingRobot
    // CSummoned::TryDoingSummon
    BEHOLDER(0xB5),

    // 0xB6 CSummoned::SendRemove

    // CVecCtrlDragon::EndUpdateActive
    MOVE_DRAGON(0xB9),

    // CQuickslotKeyMappedMan::SaveQuickslotKeyMap
    CHANGE_QUICKSLOT(0xBB),
    MOVE_LIFE(0xC2),

    // CMob::ApplyControl
    AUTO_AGGRO(0xC3),

    // 0xC4 - CMob::SendDropPickUpRequest

    // CMob::Update
    FIELD_DAMAGE_MOB(0xC5),

    // CMob::Update
    MOB_DAMAGE_MOB_FRIENDLY(0xC6),

    // CMob::TryFirstSelfDestruction
    MONSTER_BOMB(0xC7),

    // CMob::SetDamagedByMob
    MOB_DAMAGE_MOB(0xC8),

    // 0xC9 CMob::Update
    // 0xCA CMob::UpdateTimeBomb
    // 0xCB CMob::SendCollisionEscort
    // 0xCC CMob::SendRequestEscortPath
    // 0xCD CMob::SendEscortStopEndRequest

    // CNpc::GenerateMovePath
    NPC_ACTION(0xD0),

    // 0xDA - CUserLocal::ResetNLCPQ

    //0xD1 - CNpc::RequestSpecialAction

    // CWvsContext::SendDropPickUpRequest
    ITEM_PICKUP(0xD5),

    // CReactorPool::FindHitReactor
    // CReactorPool::FindSkillReactor
    DAMAGE_REACTOR(0xD8),

    // CReactorPool::FindTouchReactorAroundLocalUser
    TOUCHING_REACTOR(0xD9),
    PLAYER_MAP_TRANSFER(0xDA),

    // CField_SnowBall::Update
    // CField_SnowBall::BasicActionAttack
    SNOWBALL(0xDF),
    LEFT_KNOCKBACK(0xFFFF),

    // CField_Coconut::BasicActionAttack
    COCONUT(0xE0),

    // 0xE2 CField_GuildBoss::BasicActionAttack
    MATCH_TABLE(0xE3),

    // CUIMonsterCarnival::RequestSend
    MONSTER_CARNIVAL(0xE5),
    PARTY_SEARCH_REGISTER(0xE6),

    // CWvsContext::SendPartyWanted
    PARTY_SEARCH_START(0xE9),

    // CWvsContext::SendCancelPartyWanted
    PARTY_SEARCH_UPDATE(0xEA),

    // 0xE7 CField_ContiMove::Init
    // 0xEC CStage::OnSetField
    // 0xED CField::OnRequestFootHoldInfo

    // 0xF3 - CCashShop::OnStatusCharge

    //  CCashShop::TrySendQueryCashRequest

    CHECK_CASH(0xF4),

    // CCashShop::SendBuyAvatarPacket
    // CCashShop::SendGiftsPacket
    // CCashShop::RequestCashPurchaseRecord
    // CCashShop::OnRebateLockerItem
    // CCashShop::OnExItemSlot
    // CCashShop::OnIncTrunkCount
    // CCashShop::OnIncCharacterSlotCount
    // CCashShop::OnBuyCharacter
    // CCashShop::OnEnableEquipSlotExt
    // CCashShop::OnBuy
    // CCashShop::OnBuyPackage
    // CCashShop::OnBuyNormal
    // CCashShop::OnGiftMateInfoResult
    // CCashShop::OnBuyCouple
    // CCashShop::OnBuySlotInc
    // CCashShop::OnBuyFriendship
    // CCashShop::OnSetWish
    // CCashShop::OnRemoveWish
    // CCashShop::OnMoveCashItemLtoS
    // CCashShop::OnMoveCashItemStoL
    // CCashShop::GiftWishItem
    // CCashShop::ApplyWishListEvent
    // CCashShop::SendBuyTransferWorldItemPacket
    // sub_48BA3F
    CASHSHOP_OPERATION(0xF5),

    // CCashShop::OnStatusCoupon
    COUPON_CODE(0xF6),

    // 0xF7 - sub_48182A sub_481F71
    // 0xF8 - sub_47F824
    // 0xF9 - sub_485179

    // CUIRaiseWndBase::OnCreate
    OPEN_ITEMUI(0xFFFF),
    CLOSE_ITEMUI(0xFFFF),
    USE_ITEMUI(0xFFFF),
    // 0x100 CUIRaiseWndBase::CUIRaiseWndBase
    // CUIRaiseWndBase::~CUIRaiseWndBase
    // 0x101 CUIRaiseWnd::SendPutItem
    // 0x102 CUIRaisePieceWnd::SendPutItem
    // 0x104 sub_9C2F45
    // 0x105 CUIWebEvent2::SendRequestAuthKey2
    // 0x106 CUIWebEvent::SendRequestAuthKey
    // 0x107 CClassCompetition::SendRequestAuthKey
    // 0x108 CUIWebEvent::SendRequestAuthKey

    // 0x10A CITC::OnStatusCharge
    // 0x10B CITC::TrySendQueryCashRequest

    // CITC::OnRegisterSaleEntry
    // CITC::OnSaleCurrentItem
    // CITC::OnChangedCategory
    // CITC::OnChangedCategorySub
    // CITC::OnChangedPage
    // CITC::OnRegisterWishEntry
    // CITC::OnBuy
    // CITC::OnBuyAuctionImm
    // CITC::OnSetZzim
    // CITC::OnBuyZzim
    // CITC::OnDeleteZzim
    // CITC::OnViewWish
    // CITC::OnBuyWish
    // CITC::OnCancelWish
    // CITC::OnCancelSaleItem
    // CITC::OnMoveITCPurchaseItemLtoS
    // CITCWnd_Tab::OnButtonClicked
    // CITCBidAuctionDlg::OnButtonClicked
    MTS_OPERATION(0x10C),
    USE_MAPLELIFE(0x10E),

    // 0x10F sub_575186
    // CUIItemUpgrade::Update
    USE_HAMMER(0x114);

    // 0x116 CUILogoutGift::OnButtonClicked

    private int code;

    RecvOpcode(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }
}
