package com.noahbres.meepmeep.roadrunner.entity

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.trajectory.Trajectory
import com.acmerobotics.roadrunner.trajectory.constraints.DriveConstraints
import com.noahbres.meepmeep.core.MeepMeep
import com.noahbres.meepmeep.core.colorscheme.ColorScheme
import com.noahbres.meepmeep.core.entity.BotEntity
import com.noahbres.meepmeep.roadrunner.DriveShim
import com.noahbres.meepmeep.roadrunner.DriveTrainType
import com.noahbres.meepmeep.roadrunner.toMeepMeepPose
import com.noahbres.meepmeep.roadrunner.trajectorysequence.TrajectorySequence
import com.noahbres.meepmeep.roadrunner.trajectorysequence.sequencestep.TrajectoryStep
import com.noahbres.meepmeep.roadrunner.trajectorysequence.sequencestep.TurnStep

class RoadRunnerBotEntity(
        meepMeep: MeepMeep<*>,
        private var constraints: DriveConstraints,
        width: Double, height: Double,
        private var trackWidth: Double,
        pose: Pose2d,
        private val colorScheme: ColorScheme,
        opacity: Double
) : BotEntity(meepMeep, width, height, pose.toMeepMeepPose(), colorScheme, opacity) {
    override val zIndex: Int = 4

    private var driveTrainType = DriveTrainType.MECANUM
    var drive = DriveShim(driveTrainType, constraints, trackWidth)

    private var followMode = FollowMode.TRAJECTORY_LIST

    private var currentTrajectoryList = emptyList<Trajectory>()
    private var currentTrajectorySequence: TrajectorySequence? = null

    private var currentTrajectorySequenceIndex = 0

    private var trajectorySequenceEntity: TrajectorySequenceEntity? = null

    private var looping = true
    private var running = false

    private var trajectorySequenceElapsedTime = 0.0

    private val SKIP_LOOPS = 2
    private var skippedLoops = 0

    override fun update(deltaTime: Long) {
        if (!running) return

        if(skippedLoops++ < SKIP_LOOPS) return

        if (followMode == FollowMode.TRAJECTORY_LIST) {

        } else if (followMode == FollowMode.TRAJECTORY_SEQUENCE && currentTrajectorySequence != null) {
            val currentStep = currentTrajectorySequence?.get(currentTrajectorySequenceIndex)

            trajectorySequenceElapsedTime += deltaTime / 1000.0

            if(trajectorySequenceElapsedTime <= currentTrajectorySequence!!.duration) {
                val (currentStateStep, currentStateOffset) = currentTrajectorySequence!!.getCurrentState(trajectorySequenceElapsedTime)
                when(currentStateStep) {
                    is TrajectoryStep -> {
                        pose = currentStateStep.trajectory[currentStateOffset].toMeepMeepPose()
                    }
                    is TurnStep -> {
                       pose = Pose2d(pose.x, pose.y, currentStateStep.motionProfile[currentStateOffset].x).toMeepMeepPose()
                    }
                }
            } else if(looping) {
                trajectorySequenceElapsedTime = 0.0
            } else {
                trajectorySequenceElapsedTime = 0.0
                currentTrajectorySequence = null
            }
        }
    }

    fun start() {
        running = true
        trajectorySequenceElapsedTime = 0.0
    }

    fun followTrajectorySequence(sequence: TrajectorySequence) {
        followMode = FollowMode.TRAJECTORY_SEQUENCE

        currentTrajectorySequence = sequence

        trajectorySequenceEntity = TrajectorySequenceEntity(meepMeep, sequence, colorScheme)
        meepMeep.addEntity(trajectorySequenceEntity!!)
    }

    fun followTrajectoryList(trajectoryList: List<Trajectory>) {
        followMode = FollowMode.TRAJECTORY_LIST

        currentTrajectoryList = trajectoryList
    }

    fun setTrackWidth(trackWidth: Double) {
        this.trackWidth = trackWidth

        drive = DriveShim(driveTrainType, constraints, trackWidth)
    }

    fun setConstraints(constraints: DriveConstraints) {
        this.constraints = constraints

        drive = DriveShim(driveTrainType, constraints, trackWidth)
    }

    fun setDriveTrainType(driveTrainType: DriveTrainType) {
        this.driveTrainType = driveTrainType

        drive = DriveShim(driveTrainType, constraints, trackWidth)
    }

    enum class FollowMode {
        TRAJECTORY_LIST,
        TRAJECTORY_SEQUENCE
    }
}