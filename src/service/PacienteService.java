package service;

import model.Paciente;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PacienteService {

    private final List<Paciente> pacientes = new ArrayList<>();
    private int proximoId = 1;

    public Paciente cadastrar(
            String nome,
            String sintoma,
            String prioridade) {

        Paciente paciente = new Paciente();

        paciente.setId(proximoId++);
        paciente.setNome(nome);
        paciente.setSintoma(sintoma);
        paciente.setPrioridade(prioridade);
        paciente.setEstado("EM_FILA");
        paciente.setHoraChegada(LocalDateTime.now());

        pacientes.add(paciente);

        return paciente;
    }

    public List<Paciente> listarFila() {

        return pacientes.stream()
                .filter(p -> "EM_FILA".equals(p.getEstado()))
                .sorted(
                        Comparator
                                .comparingInt(this::pesoPrioridade)
                                .thenComparing(Paciente::getHoraChegada)
                )
                .toList();
    }

    private int pesoPrioridade(Paciente paciente) {

        switch (paciente.getPrioridade().toLowerCase()) {

            case "vermelho":
                return 0;

            case "amarelo":
                return 1;

            case "verde":
                return 2;

            default:
                return 3;
        }
    }

    public Paciente buscarPorId(int id) {

        for (Paciente paciente : pacientes) {

            if (paciente.getId() == id) {
                return paciente;
            }
        }

        return null;
    }

    public List<Paciente> listarTodos() {
        return pacientes;
    }
}