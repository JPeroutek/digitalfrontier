package com.partatoes.digitalfrontier.entity.custom;

import com.partatoes.digitalfrontier.DigitalFrontier;
import com.partatoes.digitalfrontier.entity.ModEntities;
import com.partatoes.digitalfrontier.grid.arrival.GridArrival;
import com.partatoes.digitalfrontier.grid.gen.GridChunkGenerator;
import com.partatoes.digitalfrontier.grid.jobs.JobTrades;
import com.partatoes.digitalfrontier.grid.jobs.Pricing;
import com.partatoes.digitalfrontier.grid.jobs.ProgramJob;
import com.partatoes.digitalfrontier.grid.regime.Regime;
import com.partatoes.digitalfrontier.grid.regime.RegimeRules;
import com.partatoes.digitalfrontier.grid.regime.Sentiment;
import com.partatoes.digitalfrontier.item.ModItems;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * An ordinary program: strolls the streets of the grid. Each has a sentiment toward the regime (see {@link Sentiment})
 * that decides how it treats it: loyal programs seek out the regime and report rebel players to it, rebels avoid it
 * and are hunted by it, and the most set against it fight back. Recruiters work on programs to conscript them; players
 * sway them with gifts, by defending them, and by hurting them. Programs drift into decay on decayed ground (see
 * {@link Decay}). A program with a {@link ProgramJob} keeps a post instead: it stays near home, trades, and is never
 * worn down by decay or recruited.
 */
public class ProgramEntity extends PathfinderMob implements Merchant {
    /** Whether programs drift into decay (the {@code digitalfrontier:program_decay} game rule). */
    public static final GameRule<Boolean> DECAY_RULE = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.MOBS)
            .buildAndRegister(Identifier.fromNamespaceAndPath(DigitalFrontier.MOD_ID, "program_decay"));
    // Synced: the client jitters and frays programs by it
    private static final EntityDataAccessor<Float> DECAY = SynchedEntityData.defineId(ProgramEntity.class, EntityDataSerializers.FLOAT);
    private static final double REPORT_RANGE = 12;
    private static final double ALERT_RANGE = 32;
    private static final int REPORT_COOLDOWN = 200;
    // Synced: the client gives a job program its job's skin
    private static final EntityDataAccessor<String> JOB = SynchedEntityData.defineId(ProgramEntity.class, EntityDataSerializers.STRING);
    // How far a job program may wander from home
    private static final int POST_RANGE = 6;
    private static final int KEEPER_LINES = 3;

    // NaN until first worked out from where the program lives, which is also how programs from before sentiment get one
    private float sentiment = Float.NaN;
    private int exposure;
    private int lastExposed = Integer.MIN_VALUE / 2;
    private int reportCooldown;
    @Nullable
    private BlockPos home;
    @Nullable
    private Player tradingPlayer;
    private MerchantOffers offers = new MerchantOffers();
    // Trades made today, per offer; the stock refreshes each day
    private int[] usesToday = new int[0];
    private long tradeDay = -1;

    public ProgramEntity(EntityType<? extends PathfinderMob> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DECAY, 0f);
        builder.define(JOB, "");
    }

    /** Its job, or null for an ordinary program. Works on the client too. */
    @Nullable
    public ProgramJob job() {
        return ProgramJob.byName(entityData.get(JOB));
    }

    /** Gives it a job, with its post at {@code home}: it stays, wears its title, and trades. */
    public void setJob(ProgramJob job, BlockPos home) {
        entityData.set(JOB, job.getSerializedName());
        this.home = home;
        setPersistenceRequired();
        setCustomName(Component.translatable(job.translationKey()));
        setCustomNameVisible(true);
    }

    /** How far it has drifted into decay, 0 to 1 (see {@link Decay}). Works on the client too. */
    public float decay() {
        return entityData.get(DECAY);
    }

    public void setDecay(float decay) {
        entityData.set(DECAY, Math.clamp(decay, 0f, 1f));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, EnforcerEntity.class, 10, 1.0, 1.3, entity -> stance() == Sentiment.Stance.REBEL));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RecruiterEntity.class, 8, 1.0, 1.2, entity -> stance() == Sentiment.Stance.REBEL));
        this.goalSelector.addGoal(3, new KeepPostGoal());
        this.goalSelector.addGoal(4, new SeekRegimeGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 15));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // The resistance goes for recruiters, and for enforcers attacking someone
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, RecruiterEntity.class, true,
                (target, world) -> stance() == Sentiment.Stance.RESISTANCE));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, EnforcerEntity.class, true,
                (target, world) -> stance() == Sentiment.Stance.RESISTANCE
                        && (target instanceof Mob enforcer && (enforcer.getTarget() instanceof Player || enforcer.getTarget() instanceof ProgramEntity))));
    }

    public static AttributeSupplier.Builder createProgramAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    public float sentiment() {
        return Float.isNaN(sentiment) ? 0 : sentiment;
    }

    public void setSentiment(float sentiment) {
        this.sentiment = Sentiment.clamp(sentiment);
    }

    public Sentiment.Stance stance() {
        return Sentiment.stance(sentiment());
    }

    public void shiftSentiment(float delta) {
        setSentiment(sentiment() + delta);
    }

    /** A player was kind to it ({@code strength} above 0) or hurt it (below 0). */
    public void actedOnByPlayer(Player player, float strength) {
        setSentiment(Sentiment.byPlayer(sentiment(), Regime.alignment(player), strength));
    }

    /** A second near a recruiter; every so often that's an attempt to conscript it. */
    public void expose(ServerLevel world, int ticks) {
        if (job() != null) {
            return;
        }
        RegimeRules.Recruiting recruiting = Regime.rules().recruiting();
        lastExposed = tickCount;
        setSentiment(Sentiment.exposed(sentiment(), Regime.rules().sentiment().exposurePerSecond()));
        exposure += ticks;
        if (exposure >= recruiting.exposureTicks()) {
            exposure -= recruiting.exposureTicks();
            Regime.recruit(world, this, recruiting.exposureChance());
        }
    }

    /** Whether a recruiter has been working on it in the last few seconds. */
    public boolean recentlyExposed() {
        return tickCount - lastExposed < 60;
    }

    /** A recruiter's bribe: taken, it joins the regime; refused, it holds the offer against the regime. */
    public void offerBribe(ServerLevel world) {
        if (job() == null && !Regime.recruit(world, this, Regime.rules().recruiting().bribeChance())) {
            shiftSentiment(Regime.rules().sentiment().bribeRefused());
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        super.customServerAiStep(world);
        if (Float.isNaN(sentiment)) {
            rollSentiment(world);
        }
        if (tickCount % Decay.INTERVAL == 0 && drift(world)) {
            return;
        }
        if (tickCount % 20 != 0) {
            return;
        }
        // Away from recruiters, their work wears off at half the rate it builds up
        if (!recentlyExposed()) {
            exposure = Math.max(0, exposure - 10);
        }
        if (reportCooldown > 0) {
            reportCooldown -= 20;
        } else if (stance() == Sentiment.Stance.LOYAL) {
            reportRebels(world);
        }
    }

    // One step of decay; true if it went all the way, and the program is now a corrupted one
    private boolean drift(ServerLevel world) {
        GridChunkGenerator generator = GridArrival.generator(world);
        if (generator == null || job() != null || !world.getGameRules().get(DECAY_RULE)) {
            return false;
        }
        float ground = generator.layout().planAt(blockPosition().getX() >> 4, blockPosition().getZ() >> 4).corruption();
        int corrupted = world.getEntitiesOfClass(CorruptedProgramEntity.class, getBoundingBox().inflate(8), Entity::isAlive).size();
        setDecay(Decay.step(decay(), ground, corrupted));
        if (decay() < 1) {
            return false;
        }
        CorruptedProgramEntity turned = convertTo(ModEntities.CORRUPTED_PROGRAM, ConversionParams.single(this, false, false), program -> {
            program.setProvokable(ground >= 0.7f);
            program.setPersistenceRequired();
        });
        if (turned != null) {
            world.sendParticles(ParticleTypes.SMOKE, turned.getX(), turned.getEyeY(), turned.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
            world.playSound(null, turned.getX(), turned.getY(), turned.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.NEUTRAL, 0.8f, 0.6f);
        }
        return true;
    }

    private void rollSentiment(ServerLevel world) {
        GridChunkGenerator generator = GridArrival.generator(world);
        float control = 0;
        float regime = 0.5f;
        if (generator != null) {
            control = generator.layout().planAt(blockPosition().getX() >> 4, blockPosition().getZ() >> 4).control();
            regime = generator.layout().settings().regime();
        }
        setSentiment(Sentiment.initial(random.nextDouble(), random.nextDouble(), random.nextDouble(), control, regime));
    }

    // A loyal program that sees a player the regime counts as a rebel calls the nearby enforcers on them
    private void reportRebels(ServerLevel world) {
        float reportedBelow = Regime.rules().alignment().reportedBelow();
        Player rebel = world.getNearestPlayer(getX(), getY(), getZ(), REPORT_RANGE,
                player -> !player.isSpectator() && !((Player) player).isCreative() && Regime.alignment(player) < reportedBelow
                        && hasLineOfSight(player));
        if (rebel == null) {
            return;
        }
        reportCooldown = REPORT_COOLDOWN;
        var enforcers = world.getEntitiesOfClass(EnforcerEntity.class, getBoundingBox().inflate(ALERT_RANGE), enforcer -> enforcer.getTarget() == null);
        enforcers.forEach(enforcer -> enforcer.setTarget(rebel));
        if (!enforcers.isEmpty()) {
            world.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + 0.4, getZ(), 2, 0.2, 0.1, 0.2, 0);
            playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.3f);
        }
    }

    // A gift of Luminessence
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ProgramJob job = job();
        if (job != null) {
            if (level() instanceof ServerLevel && isAlive() && tradingPlayer == null) {
                if (job == ProgramJob.PORTAL_KEEPER) {
                    player.sendSystemMessage(Component.translatable(job.translationKey() + ".line" + random.nextInt(KEEPER_LINES))
                            .withStyle(ChatFormatting.GRAY));
                }
                startTrading(player, job);
            }
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.LUMINESSENCE)) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel world) {
            actedOnByPlayer(player, Regime.rules().sentiment().gift());
            stack.consume(1, player);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getEyeY() + 0.3, getZ(), 5, 0.3, 0.2, 0.3, 0);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    // Its job's offers, priced for this player by how far apart they stand on the regime (see Pricing)
    private void startTrading(Player player, ProgramJob job) {
        List<JobTrades.Offer> jobOffers = JobTrades.offers(job);
        long day = level().getGameTime() / 24000;
        if (day != tradeDay || usesToday.length != jobOffers.size()) {
            tradeDay = day;
            usesToday = new int[jobOffers.size()];
        }
        float multiplier = Pricing.multiplier(Regime.alignment(player), sentiment());
        offers = new MerchantOffers();
        for (int i = 0; i < jobOffers.size(); i++) {
            offers.add(jobOffers.get(i).toMerchantOffer(multiplier, usesToday[i]));
        }
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 0);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        int index = offers.indexOf(offer);
        if (index >= 0 && index < usesToday.length) {
            usesToday[index]++;
        }
        playSound(SoundEvents.VILLAGER_YES, 1.0f, 1.3f);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    @Override
    public boolean stillValid(Player player) {
        return tradingPlayer == player && isAlive() && distanceToSqr(player) < 64;
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !isInvulnerableTo(world, source)) {
            actedOnByPlayer(player, -Regime.rules().sentiment().hurt());
        }
        return super.hurtServer(world, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        if (!Float.isNaN(sentiment)) {
            view.putFloat("Sentiment", sentiment);
        }
        view.putInt("Exposure", exposure);
        view.putFloat("Decay", decay());
        ProgramJob job = job();
        if (job != null && home != null) {
            view.putString("Job", job.getSerializedName());
            view.store("Home", BlockPos.CODEC, home);
            view.putLong("TradeDay", tradeDay);
            view.putIntArray("UsesToday", usesToday);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        float saved = view.getFloatOr("Sentiment", Float.NaN);
        sentiment = Float.isNaN(saved) ? Float.NaN : Sentiment.clamp(saved);
        exposure = view.getIntOr("Exposure", 0);
        setDecay(view.getFloatOr("Decay", 0f));
        ProgramJob job = view.getString("Job").map(ProgramJob::byName).orElse(null);
        if (job != null) {
            entityData.set(JOB, job.getSerializedName());
            home = view.read("Home", BlockPos.CODEC).orElse(blockPosition());
            tradeDay = view.getLongOr("TradeDay", -1);
            usesToday = view.getIntArray("UsesToday").orElse(new int[0]);
        }
    }

    // A program with a job goes back to its post when it strays
    private class KeepPostGoal extends Goal {
        KeepPostGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return home != null && job() != null && tradingPlayer == null && home.distToCenterSqr(position()) > POST_RANGE * POST_RANGE;
        }

        @Override
        public void start() {
            getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
        }

        @Override
        public boolean canContinueToUse() {
            return home != null && !getNavigation().isDone() && home.distToCenterSqr(position()) > 4;
        }
    }

    // Loyal programs drift toward the regime's people when they're about
    private class SeekRegimeGoal extends Goal {
        @Nullable
        private LivingEntity company;

        SeekRegimeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (stance() != Sentiment.Stance.LOYAL || random.nextInt(40) != 0) {
                return false;
            }
            company = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
                            entity -> entity instanceof EnforcerEntity || entity instanceof RecruiterEntity).stream()
                    .min(Comparator.comparingDouble(ProgramEntity.this::distanceToSqr))
                    .filter(entity -> distanceToSqr(entity) > 25)
                    .orElse(null);
            return company != null;
        }

        @Override
        public void start() {
            getNavigation().moveTo(company, 0.7);
        }

        @Override
        public boolean canContinueToUse() {
            return company != null && company.isAlive() && distanceToSqr(company) > 9 && !getNavigation().isDone();
        }

        @Override
        public void stop() {
            company = null;
        }
    }
}
