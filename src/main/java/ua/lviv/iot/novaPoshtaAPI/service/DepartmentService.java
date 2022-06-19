package ua.lviv.iot.novaPoshtaAPI.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.lviv.iot.novaPoshtaAPI.datastorage.dal.DepartmentFileStore;
import ua.lviv.iot.novaPoshtaAPI.model.Courier;
import ua.lviv.iot.novaPoshtaAPI.model.Department;
import ua.lviv.iot.novaPoshtaAPI.model.Parcel;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Service
public class DepartmentService {

    @Autowired
    private ParcelService parcelService;
    @Autowired
    private CourierService courierService;
    @Autowired
    private DepartmentFileStore departmentFileStore;

    private HashMap<Long, Department> departments = new HashMap<>();

    public List<Department> getAllDepartments() {
        return new LinkedList<>(this.departments.values());
    }

    public Department getDepartmentById(Long departmentId) {
        return this.departments.get(departmentId);
    }

    public void addDepartment(Department department) {
        List<Long> parcelIds = new LinkedList<>();
        department.setParcelIds(parcelIds);
        this.departments.put(department.getDepartmentId(), department);
    }

    public void updateDepartment(Department department, Long departmentId) {
        this.departments.put(departmentId, department);
    }

    public void deleteDepartment(Long departmentId) {
        for (Long parcelId: this.departments.get(departmentId).getParcelIds()) {
            parcelService.deleteParcel(parcelId);
        }
        this.departments.remove(departmentId);
    }

    public void giveParcelToCourier(Long departmentId, Long courierId, Long parcelId) {
        if (departments.get(departmentId).getParcelIds().contains(parcelId)
                && Objects.equals(courierService.getCourierById(courierId).getDepartmentId(), departmentId)) {
            List<Long> newIdsDpt = departments.get(departmentId).getParcelIds();
            newIdsDpt.remove(parcelId);
            departments.get(departmentId).setParcelIds(newIdsDpt);
            List<Long> newIdsCourier = courierService.getCourierById(courierId).getParcelIds();
            newIdsCourier.add(parcelId);
            Courier courier = courierService.getCourierById(courierId);
            courier.setParcelIds(newIdsCourier);
            courierService.updateCourier(courier, courierId);
            Parcel newParcel = parcelService.getParcelById(parcelId);
            newParcel.setLocation("Courier is delivering the parcel");
            parcelService.deleteParcel(parcelId);
            parcelService.addParcel(newParcel);
        }
    }

    public void addParcel(Long departmentId, Parcel parcel) {
        List<Long> newIds = this.departments.get(departmentId).getParcelIds();
        newIds.add(parcel.getParcelId());
        this.departments.get(departmentId).setParcelIds(newIds);
        parcel.setLocation(this.departments.get(departmentId).getLocation());
        parcelService.addParcel(parcel);
    }

    public void updateParcel(Long departmentId, Parcel parcel, Long parcelId) {
        if (this.departments.get(departmentId).getParcelIds().contains(parcel.getParcelId())) {
            parcelService.updateParcel(parcel, parcelId);
        }
    }

    public void deleteParcel(Long departmentId, Long parcelId) {
        if (this.departments.get(departmentId).getParcelIds().contains(parcelId)) {
            parcelService.deleteParcel(parcelId);
        }
    }

    public List<Parcel> getAllParcels(Long departmentId) {
        List<Parcel> result = new LinkedList<>();
        for (Parcel parcel: parcelService.getAllParcels()) {
            if (departments.get(departmentId).getParcelIds().contains(parcel.getParcelId())) {
                result.add(parcel);
            }
        }

        return result;
    }

    public Parcel getParcelById(Long departmentId, Long parcelId) {
        Parcel result = new Parcel();
        for (Parcel parcel: parcelService.getAllParcels()) {
            if (departments.get(departmentId).getParcelIds().contains(parcel.getParcelId())) {
                if (Objects.equals(parcel.getParcelId(), parcelId)) {
                    result = parcel;
                }
            }
        }

        return result;
    }

    public void deliverParcel(Long departmentIdFrom, Long departmentIdTo, Long parcelId) {
        if (departments.get(departmentIdFrom).getParcelIds().contains(parcelId)) {
            List<Long> newIdsFrom = departments.get(departmentIdFrom).getParcelIds();
            newIdsFrom.remove(parcelId);
            departments.get(departmentIdFrom).setParcelIds(newIdsFrom);
            List<Long> newIdsTo = departments.get(departmentIdTo).getParcelIds();
            newIdsTo.add(parcelId);
            departments.get(departmentIdTo).setParcelIds(newIdsTo);
            Parcel newParcel = parcelService.getParcelById(parcelId);
            newParcel.setLocation(departments.get(departmentIdTo).getLocation());
            parcelService.deleteParcel(parcelId);
            parcelService.addParcel(newParcel);
        }
    }

    public void addCourier(Courier courier) {
        if (this.departments.get(courier.getDepartmentId()) != null) {
            courierService.addCourier(courier);
        }
    }

    public void updateCourier(Courier courier, Long courierId) {
        if (this.departments.get(courier.getDepartmentId()) != null) {
            courierService.updateCourier(courier, courierId);
        }
    }

    public void deleteCourier(Long courierId) {
        courierService.deleteCourier(courierId);
    }

    @PreDestroy
    private void saveDepartments() throws IOException {
        departmentFileStore.saveDepartments(this.departments, "res\\");
    }

    @PostConstruct
    private void loadDepartments() throws IOException {
        if (departmentFileStore.loadDepartments("res\\") != null) {
            this.departments = departmentFileStore.loadDepartments("res\\");
        }
    }

}
