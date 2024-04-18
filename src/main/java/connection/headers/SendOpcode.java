package connection.headers;

public enum SendOpcode {

   LOGIN_STATUS(0x00), // CLogin::OnCheckPasswordResult
   GUEST_ID_LOGIN(0x01), // CLogin::OnGuestIDLoginResult
   WORLD_INFORMATION(0x02), // CLogin::OnWorldInformation
   CHARLIST(0x03), // CLogin::OnSelectWorldResult
   SERVER_IP(0x04), // CLogin::OnSelectCharacterResult
   CHAR_NAME_RESPONSE(0x05), // CLogin::OnCheckDuplicatedIDResult
   ADD_NEW_CHAR_ENTRY(0x06), // CLogin::OnCreateNewCharacterResult
   DELETE_CHAR_RESPONSE(0x07), // CLogin::OnDeleteCharacterResult

   ACCOUNT_INFO(0xFFFF), // CLogin::OnAccountInfoResult
   SERVERSTATUS(0xFFFF), //CLogin::OnCheckUserLimitResult
   GENDER_DONE(0xFFFF), // CLogin::OnSetAccountResult
   CONFIRM_EULA_RESULT(0xFFFF), // CLogin::OnConfirmEULAResult
   CHECK_PINCODE(0xFFFF), // CLogin::OnCheckPinCodeResult
   UPDATE_PINCODE(0xFFFF), // CLogin::OnUpdatePinCodeResult
   VIEW_ALL_CHAR(0x14), // CLogin::OnViewAllCharResult
   SELECT_CHARACTER_BY_VAC(0xFFFF), // CLogin::OnSelectCharacterByVACResult
   CHANGE_CHANNEL(0x08), // CClientSocket::OnMigrateCommand
   PING(0x9), // CClientSocket::OnAliveReq
   KOREAN_INTERNET_CAFE_SHIT(0xFFFF),
   CHANNEL_SELECTED(0xFFFF),
   HACKSHIELD_REQUEST(0xFFFF),
   RELOG_RESPONSE(0xFFFF), // sub_633496

   LOGIN_AUTH(0x18),

   // 0x17 CLogin::OnEnableSPWResult
   CHECK_CRC_RESULT(0x0D),
   LAST_CONNECTED_WORLD(0x16), // CLogin::OnLatestConnectedWorld
   RECOMMENDED_WORLD_MESSAGE(0x17), // CLogin::OnRecommendWorldMessage
   CHECK_SPW_RESULT(0xFFFF), // CLogin::OnCheckSPWResult
   INVENTORY_OPERATION(0x1B), // CWvsContext::OnInventoryOperation
   INVENTORY_GROW(0x1C), // CWvsContext::OnInventoryGrow
   STAT_CHANGED(0x1D), // CWvsContext::OnStatChanged
   GIVE_BUFF(0x1E), // CWvsContext::OnTemporaryStatSet
   CANCEL_BUFF(0x1F), // CWvsContext::OnTemporaryStatReset
   FORCED_STAT_SET(0x20), // CWvsContext::OnForcedStatSet
   FORCED_STAT_RESET(0x21), // CWvsContext::OnForcedStatReset
   UPDATE_SKILLS(0x22), // CWvsContext::OnChangeSkillRecordResult
   SKILL_USE_RESULT(0x23), // CWvsContext::OnSkillUseResult
   FAME_RESPONSE(0x24), // CWvsContext::OnGivePopularityResult
   SHOW_STATUS_INFO(0x25), // CWvsContext::OnMessage
   OPEN_FULL_CLIENT_DOWNLOAD_LINK(0xFFFF), // CWvsContext::OnOpenFullClientDownloadLink
   MEMO_RESULT(0x26), // CWvsContext::OnMemoResult
   MAP_TRANSFER_RESULT(0x27), // CWvsContext::OnMapTransferResult
   WEDDING_PHOTO(0x28), // CWvsContext::OnAntiMacroResult
   CLAIM_RESULT(0x2A), // CWvsContext::OnClaimResult
   CLAIM_AVAILABLE_TIME(0x2B), // CWvsContext::OnSetClaimSvrAvailableTime
   CLAIM_STATUS_CHANGED(0x2C), // CWvsContext::OnClaimSvrStatusChanged
   SET_TAMING_MOB_INFO(0x02D), // CWvsContext::OnSetTamingMobInfo
   QUEST_CLEAR(0x2E), // CWvsContext::OnQuestClear
   ENTRUSTED_SHOP_CHECK_RESULT(0x2F), // CWvsContext::OnEntrustedShopCheckResult
   SKILL_LEARN_ITEM_RESULT(0x30), // CWvsContext::OnSkillLearnItemResult
   GATHER_ITEM_RESULT(0x31), // CWvsContext::OnGatherItemResult
   SORT_ITEM_RESULT(0x32), // CWvsContext::OnSortItemResult
   SUE_CHARACTER_RESULT(0xFFFF), // CWvsContext::OnSueCharacterResult

   //0x38 ??
   TRADE_MONEY_LIMIT(0xFFFF), // CWvsContext::OnTradeMoneyLimit
   SET_GENDER(0xFFFF), // CWvsContext::OnSetGender
   GUILD_BBS_PACKET(0xFFFF), // CWvsContext::OnGuildBBSPacket
   CHAR_INFO(0x35), // CWvsContext::OnCharacterInfo
   PARTY_OPERATION(0x36), // CWvsContext::OnPartyResult

   //0x38 CWvsContext::OnExpedtionResult
   BUDDYLIST(0x39), // CWvsContext::OnFriendResult

   //0x42 ??
   GUILD_OPERATION(0x3B), // CWvsContext::OnGuildResult
   ALLIANCE_OPERATION(0x3C), // CWvsContext::OnAllianceResult
   SPAWN_PORTAL(0x3D), // CWvsContext::OnTownPortal
   SERVERMESSAGE(0x3E), // CWvsContext::OnBroadcastMsg
   INCUBATOR_RESULT(0x3F), // CWvsContext::OnIncubatorResult
   SHOP_SCANNER_RESULT(0x40), // CWvsContext::OnShopScannerResult
   SHOP_LINK_RESULT(0x41), // CWvsContext::OnShopLinkResult

   MARRIAGE_REQUEST(0x42), // CWvsContext::OnMarriageRequest
   MARRIAGE_RESULT(0x43), // CWvsContext::OnMarriageResult
   WEDDING_GIFT_RESULT(0x44), // CWvsContext::OnWeddingGiftResult
   NOTIFY_MARRIED_PARTNER_MAP_TRANSFER(0x45), // CWvsContext::OnNotifyMarriedPartnerMapTransfer

   CASH_PET_FOOD_RESULT(0x46), // CWvsContext::OnCashPetFoodResult
   SET_WEEK_EVENT_MESSAGE(0x47), // CWvsContext::OnSetWeekEventMessage
   SET_POTION_DISCOUNT_RATE(0x48), // CWvsContext::OnSetPotionDiscountRate

   BRIDLE_MOB_CATCH_FAIL(0x49), // CWvsContext::OnBridleMobCatchFail
   MINIGAME_PACHINKO_UPDATE_DAMA(0x4B), // sub_B06318
   IMITATED_NPC_RESULT(0xFFFF), // CWvsContext::OnImitatedNPCResult
   IMITATED_NPC_DATA(0x55), // CNpcPool::OnNpcImitateData
   LIMITED_NPC_DISABLE_INFO(0x56), // CNpcPool::OnUpdateLimitedDisableInfo
   MONSTER_BOOK_SET_CARD(0x57), // CWvsContext::OnMonsterBookSetCard
   MONSTER_BOOK_SET_COVER(0x58), // CWvsContext::OnMonsterBookSetCover
   HOUR_CHANGED(0xFFFF), // CWvsContext::OnHourChanged
   MINIMAP_ON_OFF(0x5D), // CWvsContext::OnMiniMapOnOff
   CONSULT_AUTHKEY_UPDATE(0x5E), // CWvsContext::OnConsultAuthkeyUpdate
   CLASS_COMPETITION_AUTHKEY_UPDATE(0x5F), // CWvsContext::OnClassCompetitionAuthkeyUpdate
   WEB_BOARD_AUTHKEY_UPDATE(0x60), // CWvsContext::OnWebBoardAuthkeyUpdate
   SESSION_VALUE(0x61), // CWvsContext::OnSessionValue
   PARTY_VALUE(0x62), // CWvsContext::OnPartyValue
   FIELD_SET_VARIABLE(0x63), // CWvsContext::OnFieldSetVariable
   BONUS_EXP_CHANGED(0x64), // CWvsContext::OnBonusExpRateChanged

   // 0x60 CWvsContext::OnPotionDiscountRateChanged
   FAMILY_CHART_RESULT(0x65), // CWvsContext::OnFamilyChartResult
   FAMILY_INFO_RESULT(0x66), // CWvsContext::OnFamilyInfoResult
   FAMILY_RESULT(0x67), // CWvsContext::OnFamilyResult
   FAMILY_JOIN_REQUEST(0x68), // CWvsContext::OnFamilyJoinRequest
   FAMILY_JOIN_REQUEST_RESULT(0x69), // CWvsContext::OnFamilyJoinRequestResult
   FAMILY_JOIN_ACCEPTED(0x6A), // CWvsContext::OnFamilyJoinAccepted
   FAMILY_PRIVILEGE_LIST(0x6B), // CWvsContext::OnFamilyPrivilegeList
   FAMILY_REP_GAIN(0x6C), // CWvsContext::OnFamilyFamousPointIncResult
   FAMILY_NOTIFY_LOGIN_OR_LOGOUT(0x6D), // CWvsContext::OnFamilyNotifyLoginOrLogout
   FAMILY_SET_PRIVILEGE(0x6E), // CWvsContext::OnFamilySetPrivilege
   FAMILY_SUMMON_REQUEST(0x6F), // CWvsContext::OnFamilySummonRequest

   NOTIFY_LEVELUP(0x70), // CWvsContext::OnNotifyLevelUp
   NOTIFY_MARRIAGE(0x71), // CWvsContext::OnNotifyWedding
   NOTIFY_JOB_CHANGE(0x72), // CWvsContext::OnNotifyJobChange
   MAPLE_TV_USE_RES(0xFFFF), // CWvsContext::OnMapleTVUseRes
   AVATAR_MEGAPHONE_RESULT(0xFFFF), // CWvsContext::OnAvatarMegaphoneRes
   SET_AVATAR_MEGAPHONE(0x5A), // CWvsContext::OnSetAvatarMegaphone
   CLEAR_AVATAR_MEGAPHONE(0x5B), // CWvsContext::OnClearAvatarMegaphone
   CANCEL_NAME_CHANGE_RESULT(0xFFFF), // CWvsContext::OnCancelNameChangeResult
   CANCEL_TRANSFER_WORLD_RESULT(0xFFFF), // CWvsContext::OnCancelTransferWorldResult
   DESTROY_SHOP_RESULT(0xFFFF), // CWvsContext::OnDestroyShopResult
   FAKE_GM_NOTICE(0xFFFF), // sub_AC26E5
   SUCCESS_IN_USE_GACHAPON_BOX(0x75), // CWvsContext::OnSuccessInUsegachaponBox
   NEW_YEAR_CARD_RES(0xFFFF), // CWvsContext::OnNewYearCardRes
   RANDOM_MORPH_RES(0xFFFF), // CWvsContext::OnRandomMorphRes
   CANCEL_NAME_CHANGE_BY_OTHER(0xFFFF), // CWvsContext::OnCancelNameChangebyOther
   SET_EXTRA_PENDANT_SLOT(0xFFFF), // CWvsContext::OnSetBuyEquipExt
   SCRIPT_PROGRESS_MESSAGE(0x76), // CWvsContext::OnScriptProgressMessage
   DATA_CRC_CHECK_FAILED(0x77), // CWvsContext::OnDataCRCCheckFailed

   //0x7F CWvsContext::OnCakePieEventResult
   //0x80 CWvsContext::OnUpdateGMBoard
   //0x81 CWvsContext::OnShowSlotMessage
   //0x82 CWvsContext::OnAccountMoreInfo
   //0x83 CWvsContext::OnFindFirend
   MACRO_SYS_DATA_INIT(0x7A), // CWvsContext::OnMacroSysDataInit
   SET_FIELD(0x7B), // CStage::OnSetField
   SET_ITC(0x7C), // CStage::OnSetITC
   SET_CASH_SHOP(0x7D), // CStage::OnSetCashShop
   SET_BACK_EFFECT(0x7E), // CMapLoadable::OnSetBackEffect
   SET_MAP_OBJECT_VISIBLE(0x7F), // CMapLoadable::OnSetMapObjectVisible
   CLEAR_BACK_EFFECT(0x80), // CMapLoadable::OnClearBackEffect
   BLOCKED_MAP(0x81), // CField::OnTransferFieldReqIgnored
   BLOCKED_SERVER(0x82), // CField::OnTransferChannelReqIgnored
   FORCED_MAP_EQUIP(0x83), // CField::OnFieldSpecificData
   MULTICHAT(0x84), // CField::OnGroupMessage
   WHISPER(0x85), // CField::OnWhisper
   SPOUSE_CHAT(0xFFFF), // CField::OnCoupleMessage
   SUMMON_ITEM_INAVAILABLE(0x86), // CField::OnSummonItemInavailable
   FIELD_EFFECT(0x87), // CField::OnFieldEffect
   FIELD_OBSTACLE_ONOFF(0x88), // CField::OnFieldObstacleOnOff
   FIELD_OBSTACLE_ONOFF_LIST(0x89), // CField::OnFieldObstacleOnOffStatus
   FIELD_OBSTACLE_ALL_RESET(0x8A), // CField::OnFieldObstacleAllReset
   BLOW_WEATHER(0xFFFF), // CField::OnBlowWeather
   PLAY_JUKEBOX(0x8C), // CField::OnPlayJukeBox
   ADMIN_RESULT(0xFFFF), // CField::OnAdminResult
   OX_QUIZ(0x8E), // CField::OnQuiz
   GMEVENT_INSTRUCTIONS(0x8F), // CField::OnDesc
   CLOCK(0x90),
   CONTI_MOVE(0x91), // CField_ContiMove::OnContiMove
   CONTI_STATE(0x92), // CField_ContiMove::OnContiState
   SET_QUEST_CLEAR(0x93), // CField::OnSetQuestClear
   SET_QUEST_TIME(0x94), // CField::OnSetQuestTime
   ARIANT_RESULT(0xFFFF), // CField::OnWarnMessage
   SET_OBJECT_STATE(0x95), // CField::OnSetObjectState
   STOP_CLOCK(0x96), // CField::OnDestroyClock
   ARIANT_ARENA_SHOW_RESULT(0x97), // CField_AriantArena::OnShowResult

   // 0xA4 CField::OnStalkResult
   PYRAMID_GAUGE(0x99), // CField_Massacre::OnMassacreIncGauge
   PYRAMID_SCORE(0x9A), // CField_MassacreResult::OnMassacreResult
   QUICKSLOT_INIT(0x9B), // CQuickslotKeyMappedMan::OnInit

   // 0x9C CField::OnFootHoldInfo

   // 0x9D CField::OnRequestFootHoldInfo
   SPAWN_PLAYER(0x9E), // CUserPool::OnUserEnterField
   REMOVE_PLAYER_FROM_MAP(0x9F), // CUserPool::OnUserLeaveField
   CHATTEXT(0xA0), // CUser::OnChat
   CHATTEXT1(0xA1), // CUser::OnChat
   CHALKBOARD(0xA2), // CUser::OnADBoard
   UPDATE_CHAR_BOX(0xA3), // CUser::OnMiniRoomBalloon
   SHOW_CONSUME_EFFECT(0xA4), // CUser::SetConsumeItemEffect
   SHOW_SCROLL_EFFECT(0xA5), // CUser::ShowItemUpgradeEffect
   SPAWN_PET(0xAD), // CUser::OnPetPacket

   // 0xAE CUser::OnPetPacket
   // 0xAF CUser::OnPetPacket
   MOVE_PET(0xB0), // CPet::OnMove
   PET_CHAT(0xB1), // CPet::OnAction
   PET_NAMECHANGE(0xB2), // CPet::OnNameChanged
   PET_EXCEPTION_LIST(0xB3), // CPet::OnLoadExceptionList
   PET_COMMAND(0xB4), // CPet::OnActionCommand
   SPAWN_SPECIAL_MAPOBJECT(0xB5), // CSummonedPool::OnPacket
   REMOVE_SPECIAL_MAPOBJECT(0xB6), // CSummonedPool::OnPacket
   MOVE_SUMMON(0xB7), // CSummonedPool::OnMove
   SUMMON_ATTACK(0xB8), // CSummonedPool::OnAttack
   DAMAGE_SUMMON(0xB9), // CSummonedPool::OnHit
   SUMMON_SKILL(0xBA), // CSummonedPool::OnSkill
   SPAWN_DRAGON(0xBB), // CUser::OnDragonPacket
   MOVE_DRAGON(0xBC), // CUser::OnDragonPacket
   REMOVE_DRAGON(0xFFFF), // CUser::OnDragonPacket
   MOVE_PLAYER(0xBF), // CUserRemote::OnMove
   CLOSE_RANGE_ATTACK(0xC0), // CUserRemote::OnAttack
   RANGED_ATTACK(0xC1), // CUserRemote::OnAttack
   MAGIC_ATTACK(0xC2), // CUserRemote::OnAttack
   ENERGY_ATTACK(0xC3), // CUserRemote::OnAttack
   SKILL_EFFECT(0xC4), // CUserRemote::OnSkillPrepare
   CANCEL_SKILL_EFFECT(0xC5), // CUserRemote::OnSkillCancel
   DAMAGE_PLAYER(0xC6), // CUserRemote::OnHit
   FACIAL_EXPRESSION(0xC7), // CUserPool::OnUserRemotePacket
   SHOW_ITEM_EFFECT(0xC8), // CUserPool::OnUserRemotePacket

   // 0xC9 CUserRemote::OnShowUpgradeTombEffect
   SHOW_CHAIR(0xCA), // CUserPool::OnUserRemotePacket
   UPDATE_CHAR_LOOK(0xCB), // CUserRemote::OnAvatarModified
   SHOW_FOREIGN_EFFECT(0xCC), // CUser::OnEffect
   GIVE_FOREIGN_BUFF(0xCD), // CUserRemote::OnSetTemporaryStat
   CANCEL_FOREIGN_BUFF(0xCE), // CUserRemote::OnResetTemporaryStat
   UPDATE_PARTYMEMBER_HP(0xCF), // CUserRemote::OnReceiveHP
   GUILD_NAME_CHANGED(0xD0), // CUserRemote::OnGuildNameChanged
   GUILD_MARK_CHANGED(0xD1), // CUserRemote::OnGuildMarkChanged
   THROW_GRENADE(0xD2), // CUserRemote::OnThrowGrenade
   CANCEL_CHAIR(0xD3), // CUserLocal::OnSitResult
   SHOW_ITEM_GAIN_INCHAT(0xD5), // CUser::OnEffect
   DOJO_WARP_UP(0xD6), // CUserLocal::OnTeleport
   LUCKSACK_PASS(0xD8), // CUserLocal::OnMesoGive_Succeeded // TODO handling of this might be wrong
   LUCKSACK_FAIL(0xD9), // CUserLocal::OnMesoGive_Failed // TODO handling of this might be wrong
   MESO_BAG_MESSAGE(0xFFFF),  // TODO handling of this might be wrong
   UPDATE_QUEST_INFO(0xDC), // CUserLocal::OnQuestResult

   // 0xDD CUserLocal::OnNotifyHPDecByField
   // 0xDE nullsub_18

   PLAYER_HINT(0xDF), // CUserLocal::OnBalloonMsg

   // 0xE0 CUserLocal::OnPlayEventSound
   // 0xE1 CUserLocal::OnPlayMinigameSound
   MAKER_RESULT(0xE2), // CUserLocal::OnMakerResult
   KOREAN_EVENT(0xE3), // CUserLocal::OnOpenClassCompetitionPage

   // 0xE4??
   OPEN_UI(0xE5), // CUserLocal::OnOpenUI

   // 0xE6  // CUserLocal::OnOpenUIWithOption
   LOCK_UI(0xE7), // CUserLocal::SetDirectionMode
   DISABLE_UI(0xE8), // CUserLocal::OnSetStandAloneMode
   SPAWN_GUIDE(0xE9), // CUserLocal::OnHireTutor
   TALK_GUIDE(0xEA), // CUserLocal::OnTutorMsg
   SHOW_COMBO(0xEB), // CUserLocal::OnIncComboResponse

   // 0xEC
   // 0xED
   // 0xEE
   // 0xEF
   // 0xF0
   // 0xF1 CUserLocal::OnResignQuestReturn
   // 0xF2 CUserLocal::OnPassMateName
   // 0xF3 CUserLocal::OnRadioSchedule
   // 0xF4 CUserLocal::OnOpenSkillGuide
   // 0xF5 CUserLocal::OnNoticeMsg
   // 0xF6 CUserLocal::OnChatMsg
   // 0xF7 CUserLocal::OnBuffzoneEffect
   // 0xF8 CUserLocal::OnDamageMeter
   // 0xF9
   // 0xFA
   COOLDOWN(0xFB), // CUserLocal::OnSkillCooltimeSet
   SPAWN_MONSTER(0xFD), // CMobPool::OnMobEnterField
   KILL_MONSTER(0xFE), // CMobPool::OnMobLeaveField
   SPAWN_MONSTER_CONTROL(0xFF), // CMobPool::OnMobChangeController
   MOVE_MONSTER(0x100), // CMob::OnMove
   MOVE_MONSTER_RESPONSE(0x101), // CMob::OnCtrlAck
   APPLY_MONSTER_STATUS(0x103), // CMob::OnStatSet
   CANCEL_MONSTER_STATUS(0x104), // CMob::OnStatReset
   RESET_MONSTER_ANIMATION(0x105), // CMob::OnSuspendReset

   // 0x106 CMob::OnAffected
   DAMAGE_MONSTER(0x107), // CMob::OnDamaged

   // 0x108 CMob::OnSpecialEffectBySkill
   ARIANT_THING(0x10A), // CMobPool::OnMobCrcKeyChanged
   SHOW_MONSTER_HP(0x10B), // CMob::OnHPIndicator
   CATCH_MONSTER(0x10C), // CMob::OnCatchEffect
   CATCH_MONSTER_WITH_ITEM(0x10D), // CMob::OnEffectByItem
   SHOW_MAGNET(0x10E), // CMob::OnMobSpeaking

   // 0x10F CMob::OnMobSkillDelay
   // 0x110 CMob::OnEscortFullPath
   // 0x112 CMob::OnEscortStopSay
   // 0x113 CMob::OnEscortReturnBefore
   // 0x114 CMob::OnMobAttackedByMob
   SPAWN_NPC(0x116), // CNpcPool::OnNpcEnterField
   REMOVE_NPC(0x117), // CNpcPool::OnNpcLeaveField
   SPAWN_NPC_REQUEST_CONTROLLER(0x118), // CNpcPool::OnNpcChangeController
   NPC_ACTION(0x119), // CNpc::OnMove

   // 0x11A CNpc::OnUpdateLimitedInfo
   // 0x11B CNpc::OnSetSpecialAction
   SET_NPC_SCRIPTABLE(0x11C), // CNpcTemplate::OnSetNpcScript
   SPAWN_HIRED_MERCHANT(0x11E), // CEmployeePool::OnEmployeeEnterField
   DESTROY_HIRED_MERCHANT(0x11F), // CEmployeePool::OnEmployeeLeaveField
   UPDATE_HIRED_MERCHANT(0x120), // CEmployeePool::OnEmployeeMiniRoomBalloon
   DROP_ITEM_FROM_MAPOBJECT(0x121), // CDropPool::OnDropEnterField
   REMOVE_ITEM_FROM_MAP(0x122), // CDropPool::OnDropLeaveField
   CANNOT_SPAWN_KITE(0x123), // CMessageBoxPool::OnCreateFailed
   SPAWN_KITE(0x124), // CMessageBoxPool::OnMessageBoxEnterField
   REMOVE_KITE(0x125), // CMessageBoxPool::OnMessageBoxLeaveField
   SPAWN_MIST(0x126), // CAffectedAreaPool::OnAffectedAreaCreated
   REMOVE_MIST(0x127), // CAffectedAreaPool::OnAffectedAreaRemoved
   SPAWN_DOOR(0x128), // CTownPortalPool::OnTownPortalCreated
   REMOVE_DOOR(0x129), // CTownPortalPool::OnTownPortalRemoved
   REACTOR_HIT(0x12D), // CReactorPool::OnReactorChangeState
   REACTOR_SPAWN(0x12F), // CReactorPool::OnReactorEnterField
   REACTOR_DESTROY(0x130), // CReactorPool::OnReactorLeaveField
   SNOWBALL_STATE(0x131), // CField_SnowBall::OnSnowBallState
   HIT_SNOWBALL(0x132), // CField_SnowBall::OnSnowBallHit
   SNOWBALL_MESSAGE(0x133), // CField_SnowBall::OnSnowBallMsg
   LEFT_KNOCK_BACK(0x134), // CField_SnowBall::OnSnowBallTouch
   COCONUT_HIT(0x135), // CField_Coconut::OnCoconutHit
   COCONUT_SCORE(0x136), // CField_Coconut::OnCoconutScore
   GUILD_BOSS_HEALER_MOVE(0x137), // CField_GuildBoss::OnHealerMove
   GUILD_BOSS_PULLEY_STATE_CHANGE(0x138), // CField_GuildBoss::OnPulleyStateChange
   MONSTER_CARNIVAL_START(0x139), // CField_MonsterCarnival::OnEnter
   MONSTER_CARNIVAL_OBTAINED_CP(0x13A), // CField_MonsterCarnival::OnPersonalCP
   MONSTER_CARNIVAL_PARTY_CP(0x13B), // CField_MonsterCarnival::OnTeamCP
   MONSTER_CARNIVAL_SUMMON(0x13C), // CField_MonsterCarnival::OnRequestResult
   MONSTER_CARNIVAL_MESSAGE(0x13D), // CField_MonsterCarnival::OnRequestResult
   MONSTER_CARNIVAL_DIED(0x13E), // CField_MonsterCarnival::OnProcessForDeath
   MONSTER_CARNIVAL_LEAVE(0x13F), // CField_MonsterCarnival::OnShowMemberOutMsg

   // 0x140 CField_MonsterCarnival::OnShowGameResult

   ARIANT_ARENA_USER_SCORE(0x141), // CField_AriantArena::OnUserScore
   SHEEP_RANCH_INFO(0x143), // CField_Battlefield::OnScoreUpdate
   SHEEP_RANCH_CLOTHES(0x144), // CField_Battlefield::OnTeamChanged
   HORNTAIL_CAVE(0x145), // CField::OnHontailTimer
   WITCH_TOWER_SCORE_UPDATE(0x146), // CField_Witchtower::OnScoreUpdate
   ZAKUM_SHRINE(0x148), // CField::OnZakumTimer
   NPC_TALK(0x149), // CScriptMan::OnPacket
   OPEN_NPC_SHOP(0x14A), // CShopDlg::OnPacket
   CONFIRM_SHOP_TRANSACTION(0x14B), // CShopDlg::OnPacket
   ADMIN_SHOP_MESSAGE(0x14C), // CAdminShopDlg::OnPacket
   ADMIN_SHOP(0x14D), // CAdminShopDlg::OnPacket
   STORAGE(0x14E), // CTrunkDlg::OnPacket
   FREDRICK_MESSAGE(0x14F), // CStoreBankDlg::OnPacket
   FREDRICK(0x150), // CStoreBankDlg::OnPacket
   RPS_GAME(0x151), // CRPSGameDlg::OnPacket
   MESSENGER(0x152), // CUIMessenger::OnPacket
   PLAYER_INTERACTION(0x153), // CMiniRoomBaseDlg::OnPacketBase

   TOURNAMENT(0x154), // CField_Tournament::OnTournament
   TOURNAMENT_MATCH_TABLE(0x155), // CField_Tournament::OnTournamentMatchTable
   TOURNAMENT_SET_PRIZE(0x156), // CField_Tournament::OnTournamentSetPrize
   TOURNAMENT_UEW(0x157), // CField_Tournament::OnTournamentUEW
   TOURNAMENT_CHARACTERS(0x158), // nullsub_12

   WEDDING_PROGRESS(0x159), // CField_Wedding::OnWeddingProgress
   WEDDING_CEREMONY_END(0x15A), // CField_Wedding::OnWeddingCeremonyEnd

   PARCEL(0x160), // CParcelDlg::OnPacket

   CHARGE_PARAM_RESULT(0x161), // CCashShop::OnChargeParamResult
   QUERY_CASH_RESULT(0x163), // CCashShop::OnQueryCashResult
   CASHSHOP_OPERATION(0x164), // CCashShop::OnCashItemResult
   CASHSHOP_PURCHASE_EXP_CHANGED(0x165), // CCashShop::OnPurchaseExpChanged
   CASHSHOP_GIFT_INFO_RESULT(0x166), // CCashShop::OnGiftMateInfoResult
   CASHSHOP_CHECK_NAME_CHANGE(0xFFFF), // CCashShop::OnCheckDuplicatedIDResult
   CASHSHOP_CHECK_NAME_CHANGE_POSSIBLE_RESULT(0xFFFF), // CCashShop::OnCheckNameChangePossibleResult
   CASHSHOP_REGISTER_NEW_CHARACTER_RESULT(0xFFFF),
   CASHSHOP_CHECK_TRANSFER_WORLD_POSSIBLE_RESULT(0x16C), // CCashShop::OnCheckTransferWorldPossibleResult
   CASHSHOP_GACHAPON_STAMP_RESULT(0x16B), // CCashShop::OnCashShopGachaponStampResult
   CASHSHOP_CASH_ITEM_GACHAPON_RESULT(0x16D), // CCashShop::OnCashItemGachaponResult
   CASHSHOP_CASH_GACHAPON_OPEN_RESULT(0xFFFF),
   KEYMAP(0x170), // CFuncKeyMappedMan::OnInit
   AUTO_HP_POT(0x171), // CFuncKeyMappedMan::OnPetConsumeItemInit
   AUTO_MP_POT(0x172), // CFuncKeyMappedMan::OnPetConsumeMPItemInit
   SEND_TV(0x17A), // CMapleTVMan::OnSetMessage
   REMOVE_TV(0x17B), // CMapleTVMan::OnClearMessage
   ENABLE_TV(0x17C), // CMapleTVMan::OnSendMessageResult
   MTS_OPERATION2(0xFFFF), // CField::OnCharacterSale TODO
   MTS_OPERATION(0xFFFF), // CField::OnCharacterSale TODO
   MAPLELIFE_RESULT(0xFFFF), // TODO
   MAPLELIFE_ERROR(0xFFFF), // CField::OnItemUpgrade // TODO
   // 0x174 CField::OnItemUpgrade // TODO
   VICIOUS_HAMMER(0xFFFF),

   // 0x17A CField::OnVega
   VEGA_SCROLL(0xFFFF); // CField::OnVega
   // 0x17C CField::OnVega
   // 0x17D CField::OnVega

   private final int code;

   SendOpcode(int code) {
      this.code = code;
   }

   public int getValue() {
      return code;
   }
}
