package niaromi.domain.release.service

import niaromi.domain.release.interactor.GetApplicationRelease
import niaromi.domain.release.model.Release

interface ReleaseService {

    suspend fun latest(arguments: GetApplicationRelease.Arguments): Release?
}
