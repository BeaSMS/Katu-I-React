package com.katui.offline.data.local

import androidx.room.TypeConverter
import com.katui.offline.data.local.entities.IntensidadeFluxo
import com.katui.offline.data.local.entities.SexoBiologico
import com.katui.offline.data.local.entities.TipoLembreteConsulta
import com.katui.offline.data.local.entities.TipoSaudeItem
import com.katui.offline.data.local.entities.TipoUsuario
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, dateFormatter) }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.format(dateTimeFormatter)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it, dateTimeFormatter) }

    @TypeConverter
    fun fromSexoBiologico(value: SexoBiologico): String = value.name

    @TypeConverter
    fun toSexoBiologico(value: String): SexoBiologico = SexoBiologico.valueOf(value)

    @TypeConverter
    fun fromTipoSaudeItem(value: TipoSaudeItem): String = value.name

    @TypeConverter
    fun toTipoSaudeItem(value: String): TipoSaudeItem = TipoSaudeItem.valueOf(value)

    @TypeConverter
    fun fromTipoUsuario(value: TipoUsuario): String = value.name

    @TypeConverter
    fun toTipoUsuario(value: String): TipoUsuario = TipoUsuario.valueOf(value)

    @TypeConverter
    fun fromIntensidadeFluxo(value: IntensidadeFluxo): String = value.name

    @TypeConverter
    fun toIntensidadeFluxo(value: String): IntensidadeFluxo = IntensidadeFluxo.valueOf(value)

    @TypeConverter
    fun fromTipoLembreteConsulta(value: TipoLembreteConsulta): String = value.name

    @TypeConverter
    fun toTipoLembreteConsulta(value: String): TipoLembreteConsulta = TipoLembreteConsulta.valueOf(value)

    @TypeConverter
    fun fromMetodoNotificacao(value: com.katui.offline.data.local.entities.MetodoNotificacao): String = value.name

    @TypeConverter
    fun toMetodoNotificacao(value: String): com.katui.offline.data.local.entities.MetodoNotificacao = com.katui.offline.data.local.entities.MetodoNotificacao.valueOf(value)
}
