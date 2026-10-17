package com.mesengo.domain.usecase

import javax.inject.Inject

class CheckAuthUseCase @Inject constructor() {
    operator fun invoke(): Boolean {
        return false
    }
}
