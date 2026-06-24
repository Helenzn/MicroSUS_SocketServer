package service;

import model.Paciente;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

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
        paciente.setHoraChegada("24/06/2026 00:15");

        pacientes.add(paciente);

        return paciente;
    }


    // GET /fila
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


    // GET /pacientes
    public List<Paciente> listarTodos() {

        return pacientes;
    }


    // GET /pacientes/{id}
    public Paciente buscarPorId(int id) {

        for (Paciente paciente : pacientes) {

            if (paciente.getId() == id) {
                return paciente;
            }
        }

        return null;
    }

    // POST /pacientes/{id}/finalizar
    public String validarFinalizacao(Paciente paciente){

        if("EM_FILA".equals(paciente.getEstado())){

            return "Paciente está EM_FILA. Não é possível finalizar sem antes chamar para atendimento.";
        }


        if("ATENDIDO".equals(paciente.getEstado())){

            return "Paciente já está ATENDIDO. Não há transição possível.";
        }


        return null;
    }

    // PUT /pacientes/{id}
    public Paciente atualizar(
            int id,
            String nome,
            String sintoma,
            String prioridade) {


        Paciente paciente = buscarPorId(id);


        if (paciente == null) {
            return null;
        }


        paciente.setNome(nome);
        paciente.setSintoma(sintoma);
        paciente.setPrioridade(prioridade);


        return paciente;
    }


    // DELETE /pacientes/{id}
    public boolean remover(int id) {

        Paciente paciente = buscarPorId(id);


        if (paciente == null) {
            return false;
        }


        pacientes.remove(paciente);

        return true;
    }

    // POST /chamar
    public Paciente chamarProximo() {

        Paciente proximo = pacientes.stream()
                .filter(p -> "EM_FILA".equals(p.getEstado()))
                .sorted(
                    Comparator
                        .comparingInt(this::pesoPrioridade)
                        .thenComparing(Paciente::getHoraChegada)
                )
                .findFirst()
                .orElse(null);


        if(proximo != null){
            proximo.setEstado("EM_ATENDIMENTO");
        }


        return proximo;
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
        // GET /estatisticas
    public Map<String, Object> estatisticas() {

        Map<String, Object> resultado = new HashMap<>();


        // Total geral
        resultado.put("totalPacientes", pacientes.size());


        // Estados
        Map<String, Integer> estados = new HashMap<>();

        estados.put("EM_FILA", 0);
        estados.put("EM_ATENDIMENTO", 0);
        estados.put("ATENDIDO", 0);


        // Prioridades
        Map<String, Integer> prioridades = new HashMap<>();

        prioridades.put("vermelho", 0);
        prioridades.put("amarelo", 0);
        prioridades.put("verde", 0);



        for(Paciente p : pacientes) {


            // conta estados
            if(estados.containsKey(p.getEstado())) {

                estados.put(
                    p.getEstado(),
                    estados.get(p.getEstado()) + 1
                );
            }



            // conta prioridades
            String prioridade =
                    p.getPrioridade().toLowerCase();


            if(prioridades.containsKey(prioridade)) {

                prioridades.put(
                    prioridade,
                    prioridades.get(prioridade) + 1
                );
            }
        }


        resultado.put("porEstado", estados);

        resultado.put("porPrioridade", prioridades);


        return resultado;
    }
}